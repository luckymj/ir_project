package ir_course;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.*;

public class LuceneSearchApp_group9 {

	int allDocumentsNumber = 0;
	int relevantDocumentsNumber = 0;
	int totalRelevantDocuments = 0;
	
	static String[] analyzeModes = {
		"vsm_Stop_NoStem",
		"vsm_NoStop_PorterStem",
		"vsm_Stop_PorterStem",
		"bm25_Stop_NoStem",
		"bm25_NoStop_PorterStem",
		"bm25_Stop_PorterStem"
	};
	
	public LuceneSearchApp_group9() {}
	
	public IndexWriterConfig index(List<DocumentInCollection> docs, String analyzeMode) throws IOException {

		// Add the document to the index
		Analyzer analyzer = new StandardAnalyzer();
		
		// Store the index on disk
		Directory directory = FSDirectory.open(Paths.get("index"));
		IndexWriterConfig config = new IndexWriterConfig(analyzer);

/*		 Common Analyzers
		- WhitespaceAnalyzer : Splits tokens on whitespace
		- SimpleAnalyzer : Splits tokens on non-letters, and then lowercases
		- StopAnalyzer : Same as SimpleAnalyzer, but also removes stop words
		- StandardAnalyzer : Most sophisticated analyzer that knows about certain token types, lowercases, removes stop words, ...

		"The quick brown fox jumped over the lazy dog"
		- WhitespaceAnalyzer : [The] [quick] [brown] [fox] [jumped] [over] [the] [lazy] [dog]
		- SimpleAnalyzer     : [the] [quick] [brown] [fox] [jumped] [over] [the] [lazy] [dog]
		- StopAnalyzer       :       [quick] [brown] [fox] [jumped] [over]       [lazy] [dog] 
		- StandardAnalyzer   :       [quick] [brown] [fox] [jumped] [over]       [lazy] [dog]
*/		

		CharArraySet stopWords;
		
		switch(analyzeMode) {
		case "vsm_Stop_NoStem":
			// VSM with stop words and no stemmer
			stopWords = StandardAnalyzer.STOP_WORDS_SET; // Standard analyzer : + stop words - stemmer
			analyzer = new StandardAnalyzer(stopWords);
			config = new IndexWriterConfig(analyzer);
			config.setSimilarity(new ClassicSimilarity());
			break;
		case "vsm_NoStop_PorterStem":
			// VSM with Porter stemmer and no stop words
			analyzer = new EnglishAnalyzer(CharArraySet.EMPTY_SET);
			config = new IndexWriterConfig(analyzer);
			config.setSimilarity(new ClassicSimilarity());
			break;
		case "vsm_Stop_PorterStem":
			// VSM with Porter stemmer and stop words
			stopWords = EnglishAnalyzer.getDefaultStopSet(); // EnglishAnalyzer already has a stemmer
			analyzer = new EnglishAnalyzer(stopWords);
			config = new IndexWriterConfig(analyzer);
			config.setSimilarity(new ClassicSimilarity());
			break;
		case "bm25_Stop_NoStem":
			//BM25 with stop words and no stemmer
			stopWords = StandardAnalyzer.STOP_WORDS_SET; // Standard analyzer : + stop words - stemmer
			analyzer = new StandardAnalyzer(stopWords);
			config = new IndexWriterConfig(analyzer);
			config.setSimilarity(new BM25Similarity());				
			break;
		case "bm25_NoStop_PorterStem":
			//BM25 with Porter stemmer and no stop words
			analyzer = new EnglishAnalyzer(CharArraySet.EMPTY_SET);
			config = new IndexWriterConfig(analyzer);
			config.setSimilarity(new BM25Similarity());
			break;
		case "bm25_Stop_PorterStem":
			//BM25 with Porter stemmer and stop words
			stopWords = EnglishAnalyzer.getDefaultStopSet();
			analyzer = new EnglishAnalyzer(stopWords);
			config = new IndexWriterConfig(analyzer);
			config.setSimilarity(new BM25Similarity());			
			break;
		}

		config.setOpenMode(OpenMode.CREATE);
		IndexWriter iwriter = new IndexWriter(directory, config);

		// Indexing : title, abstract, task number, relevance
        for (DocumentInCollection documentdoc : docs) {
            Document doc = new Document();
			
            doc.add(new Field("title", documentdoc.getTitle(), TextField.TYPE_STORED));
			doc.add(new Field("abstractText", documentdoc.getAbstractText(), TextField.TYPE_STORED));
			doc.add(new Field("search_task_number", Integer.toString(documentdoc.getSearchTaskNumber()), TextField.TYPE_STORED));

			// Set relevance "1" if it is in comparison scenario 9
			if (documentdoc.isRelevant() && documentdoc.getSearchTaskNumber() == 9) {
				doc.add(new Field("relevance", "1", TextField.TYPE_STORED));	
	            relevantDocumentsNumber++;
			} else {
				doc.add(new Field("relevance", "0", TextField.TYPE_STORED));				
			}
			
            iwriter.addDocument(doc);
            allDocumentsNumber++;
        }		

		// Print total number of docs
		System.out.println("Number of total docs: " + allDocumentsNumber);
		System.out.println("Number of relevant docs: " + relevantDocumentsNumber);

		iwriter.close();
		directory.close();

		return config;
	}
	
	public List<Document> search(String searchQuery, IndexWriterConfig cf) throws IOException, ParseException {
		// Print search query
		System.out.println("searchQuery: " + searchQuery);
		
		// Search query in abstract field
		QueryParser qp = new QueryParser("abstractText", cf.getAnalyzer());
		Query stemmedQuery = null;

		try {
			stemmedQuery = qp.parse(searchQuery);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
		
		// Search only documents with task number 9 (Scenario : Speech Recognition)
		booleanQuery.add(new TermQuery(new Term("search_task_number", "9")), BooleanClause.Occur.MUST);
		booleanQuery.add(stemmedQuery, BooleanClause.Occur.MUST);
		
		List<Document> documents = new LinkedList<Document>();

		// Run Lucene search
		DirectoryReader ireader = DirectoryReader.open(FSDirectory.open(Paths.get("index")));
		IndexSearcher isearcher = new IndexSearcher(ireader);
		
		isearcher.setSimilarity(cf.getSimilarity());
		
		int topK = 3000;
		TopDocs docs = isearcher.search(booleanQuery.build(), topK);
		ScoreDoc[] scoredDocuments = docs.scoreDocs;
		
		totalRelevantDocuments = 0;
		
		// Transform search results into Document objects list
		for (ScoreDoc aDoc : scoredDocuments) {
			Document document = isearcher.doc(aDoc.doc);
			documents.add(document);
			//System.out.println("score: " + aDoc.score + " | relevance: " + document.get("relevance") + " | title: " + document.get("title") + " | abstractText: " + document.get("abstractText"));

			if (document.get("relevance").equals("1")) {
				totalRelevantDocuments++;
			}
		}

		//System.out.println("result number of total docs: " + totalRelevantDocuments);
		
		ireader.close();
		return documents;
	}
	
	public List<Point2D> getPrecisionRecall(List<Document> documents) throws IOException {
		int relevantDocumentsResult = 0;
		int nonRelevantDocumentsResult = 0;
		List<Point2D> precisionRecall = new LinkedList<Point2D>();

		//System.out.println("R\tP");
		
		for (Document document : documents) {
			if (document.get("relevance").equals("1")) {
				relevantDocumentsResult++;
			} else {
				nonRelevantDocumentsResult++;
			}

			Double recall = relevantDocumentsResult / (double) totalRelevantDocuments;
			Double precision = relevantDocumentsResult / (double) (relevantDocumentsResult + nonRelevantDocumentsResult);
			Point2D point = new Point2D.Double(recall, precision);
			precisionRecall.add(point);

			//DecimalFormat formatter = new DecimalFormat("#0.0000");
			//System.out.println(formatter.format(point.getX()) + "\t" + formatter.format(point.getY()));
		}
		
		return precisionRecall;
	}
	
	public List<Point2D> getPrecisionRecallInterpolated(List<Point2D> precisionRecall) throws IOException {
		List<Point2D> precisionRecallInterpolated = new LinkedList<>();
		ListIterator<Point2D> iterator = precisionRecall.listIterator(precisionRecall.size() - 1);

		while (iterator.hasPrevious()) {
			Point2D point = iterator.previous();
			
			if (precisionRecallInterpolated.isEmpty() || point.getY() >= precisionRecallInterpolated.get(0).getY()) {
				precisionRecallInterpolated.add(0, point);
			}
		}
		
		return precisionRecallInterpolated;
	}
	
	public List<Point2D> getPrecisionRecallElevenPoints(List<Point2D> precisionRecallInterpolated) {
		List<Point2D> precisionRecallElevenPoints = new LinkedList<>();
		Iterator<Point2D> iterator = precisionRecallInterpolated.iterator();
		
		Point2D point = iterator.next();

		for (int i = 0; i < 11;) {
			double x = i / 10.d;

			if (point.getX() >= x || !iterator.hasNext()) {
				Point2D averagePoint = new Point2D.Double(x, point.getY());
				precisionRecallElevenPoints.add(averagePoint);
				i++;
			} else {
				point = iterator.next();
			}
		}
		
		return precisionRecallElevenPoints;
	}
	
	public List<Point2D> getPrecisionRecallElevenPointsAverage(List<List<Point2D>> PrecisionRecallElevenPoints) {
		List<Point2D> precisionRecallElevenPointsAverage = new LinkedList<Point2D>();
		
		for (int i = 0; i < 11; i++) {
			int index = i;
			double sum = PrecisionRecallElevenPoints.stream()
					.mapToDouble(list -> list.get(index).getY())
					.sum();
			
			double recall = PrecisionRecallElevenPoints.get(0).get(i).getX();
			double precision = sum / PrecisionRecallElevenPoints.size();
			
			Point2D point = new Point2D.Double(recall, precision);
			precisionRecallElevenPointsAverage.add(point);
		}
		
		return precisionRecallElevenPointsAverage;
	}
	
	public void printPrecisionRecall(List<Point2D> precisionRecall) {
		System.out.println("R\tP");

		for (Point2D point : precisionRecall) {
			DecimalFormat formatter = new DecimalFormat("#0.0000");
			System.out.println(formatter.format(point.getX()) + "\t" + formatter.format(point.getY()));
		}
	}
	
	public void printPrecisionRecallForQuery(List<List<Point2D>> precisionRecallQuery) {
		System.out.println("R\tP1\tP2\tP3\tP avg");

		for (int i = 0; i < precisionRecallQuery.get(0).size(); i++) {
			DecimalFormat formatter = new DecimalFormat("#0.0000");
			StringBuilder stringBuiler = new StringBuilder();
			
			stringBuiler.append(precisionRecallQuery.get(0).get(i).getX() + "\t");
			for (List<Point2D> precisionRecall : precisionRecallQuery) {
				Point2D point = precisionRecall.get(i);
				stringBuiler.append(formatter.format(point.getY()) + "\t");
			}
			
			System.out.println(stringBuiler.toString());
		}
	}

	public static void main(String[] args) throws IOException, ParseException {
		if (args.length > 0) {
			LuceneSearchApp_group9 engine = new LuceneSearchApp_group9();
			
			// Read document collection 
			DocumentCollectionParser parser = new DocumentCollectionParser();
			parser.parse(args[0]);
			List<DocumentInCollection> docs = parser.getDocuments();

			// testing query for stemming and stopwords (test with => corpus_part2_test.xml)
			String[] queryset = { "speech recognition algorithm", "natural language speech recognition", "speech recognition research" };
			
			for (String mode : analyzeModes) {
				System.out.println("analyze mode: " + mode);
				IndexWriterConfig cf = engine.index(docs, mode);
				List<List<Point2D>> precisionRecallElevenPointsForQuery = new LinkedList<>();
				
				for (String query : queryset) {
					List<Document> documents = engine.search(query, cf);

					List<Point2D> precisionRecall = engine.getPrecisionRecall(documents);
					List<Point2D> precisionRecallInterpolated = engine.getPrecisionRecallInterpolated(precisionRecall);
					List<Point2D> precisionRecallElevenPoints = engine.getPrecisionRecallElevenPoints(precisionRecallInterpolated);

					precisionRecallElevenPointsForQuery.add(precisionRecallElevenPoints);
					
				}
				
				List<Point2D> precisionRecallElevenPointsCombined = engine.getPrecisionRecallElevenPointsAverage(precisionRecallElevenPointsForQuery);
				precisionRecallElevenPointsForQuery.add(precisionRecallElevenPointsCombined);
				
				engine.printPrecisionRecallForQuery(precisionRecallElevenPointsForQuery);
				
				System.out.println("");
			}

			
		} else {
			System.out.println("ERROR: the path of a document file has to be passed as a command line argument.");
		}
	}
}
