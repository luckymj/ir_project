/*
 * Skeleton class for the Lucene search program implementation
 *
 * Created on 2011-12-21
 * * Jouni Tuominen <jouni.tuominen@aalto.fi>
 * 
 * Modified on 2015-30-12
 * * Esko Ikkala <esko.ikkala@aalto.fi>
 * 
 */
package ir_course;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.*;
import org.apache.lucene.util.BytesRef;


public class LuceneSearchApp_group9 {

	int all_doc_num = 0;
	int rel_doc_num = 0;
	int rel_doc_result = 0;
	
	static String[] analyze_modes = {
		"vsm_Stop_NoStem",
		"vsm_NoStop_PorterStem",
		"vsm_Stop_PorterStem",
		"bm25_Stop_NoStem",
		"bm25_NoStop_PorterStem",
		"bm25_Stop_PorterStem"
	};
	
	public LuceneSearchApp_group9() {

	}
	
	public IndexWriterConfig index(List<DocumentInCollection> docs, String analyzeMode) throws IOException {

		// Add the document to the index
		Analyzer analyzer = new StandardAnalyzer();
		
		// Store the index on disk:
		// Directory directory = new RAMDirectory();
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
			stopWords = EnglishAnalyzer.getDefaultStopSet(); //EnglishAnalyzer already has a stemmer
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
			if ( documentdoc.isRelevant() && documentdoc.getSearchTaskNumber() == 9 )
			{
				doc.add(new Field("relevance", "1", TextField.TYPE_STORED));	
	            rel_doc_num++;
			}
			else
			{
				doc.add(new Field("relevance", "0", TextField.TYPE_STORED));				
			}
			
            iwriter.addDocument(doc);
            all_doc_num++;
        }		

		// Print total number of docs
		System.out.println("number of total docs: " + all_doc_num);
		System.out.println("number of relevant docs: " + rel_doc_num);
		
		iwriter.close();
		directory.close();
		
		return config;
	}
	
	public List<String> search(String searchQuery, IndexWriterConfig cf) throws IOException, ParseException {

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
		
		// search only documents with task number 9 (Scenario : Speech Recognition)
		booleanQuery.add(new TermQuery(new Term("search_task_number", "9")), BooleanClause.Occur.MUST);
		booleanQuery.add(stemmedQuery, BooleanClause.Occur.MUST);
		
		//System.out.println(stemmedQuery.getClass().getSimpleName());
		
		List<String> results = new LinkedList<String>();

		// Run Lucene search
		DirectoryReader ireader = DirectoryReader.open(FSDirectory.open(Paths.get("index")));
		// System.out.println("Docs: " + ireader.getDocCount("Title"));
		IndexSearcher isearcher = new IndexSearcher(ireader);
		
		isearcher.setSimilarity(cf.getSimilarity());
		
		int topK = 10;
		
		TopDocs docs = isearcher.search(booleanQuery.build(), topK);
		ScoreDoc[] scored = docs.scoreDocs;
		
		rel_doc_result = 0;
		// Save the results
		for (ScoreDoc aDoc : scored) {
			Document d = isearcher.doc(aDoc.doc);
			results.add("score: " + aDoc.score + " | relevance: " + d.get("relevance") + " | title: " + d.get("title") + " | abstractText: " + d.get("abstractText"));
			
			if ( new String(d.get("relevance")).equals("1"))
			{
				rel_doc_result++;
			}
		}
		
		System.out.println("result number of total docs: " + rel_doc_result);
		
		ireader.close();
		
		return results;
	}
		
	public void printResults(List<String> results) {
		if (results.size() > 0) {
			Collections.sort(results);
			for (int i=0; i<results.size(); i++)
				System.out.println(" " + (i+1) + ". " + results.get(i));
		}
		else
			System.out.println(" no results");
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		if (args.length > 0) {
			LuceneSearchApp_group9 engine = new LuceneSearchApp_group9();
			
			// Read document collection 
			DocumentCollectionParser parser = new DocumentCollectionParser();
			parser.parse(args[0]);
			List<DocumentInCollection> docs = parser.getDocuments();
				

			// testing query for stemming and stopwords (test with => corpus_part2_test.xml)
			String[] queryset = { "the speech recognitions"};			
			
			
			for (String mode : analyze_modes)
			{
				System.out.println("analyze mode: " + mode);
				IndexWriterConfig cf = engine.index(docs, mode);
				for (String query : queryset)
				{
					List<String> results;
					results = engine.search(query, cf);
					// Display the results
					engine.printResults(results);
					
				}				
				System.out.println("");
			}

			
		}
		else
			System.out.println("ERROR: the path of a document file has to be passed as a command line argument.");
	}
}
