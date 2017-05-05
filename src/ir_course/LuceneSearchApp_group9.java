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
	
	public LuceneSearchApp_group9() {

	}
	
	public IndexWriterConfig index(List<DocumentInCollection> docs) throws IOException {

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

		// VSM with Porter stemmer and stop words
//		CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet(); //EnglishAnalyzer already has a stemmer
//		analyzer = new EnglishAnalyzer(stopWords);
//		config = new IndexWriterConfig(analyzer);
//      	config.setSimilarity(new ClassicSimilarity());
        
        
		// VSM with Porter stemmer and no stop words
//		analyzer = new EnglishAnalyzer(CharArraySet.EMPTY_SET);
//      config = new IndexWriterConfig(analyzer);
//      config.setSimilarity(new ClassicSimilarity());
        
        
		// VSM with stop words and no stemmer
		CharArraySet stopWords = StandardAnalyzer.STOP_WORDS_SET; // Standard analyzer : + stop words - stemmer
		analyzer = new StandardAnalyzer(stopWords);
		config = new IndexWriterConfig(analyzer);
		config.setSimilarity(new ClassicSimilarity());
       
        
		//BM25 with Porter stemmer and stop words
//		CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();
//		analyzer = new EnglishAnalyzer(stopWords);
//		config = new IndexWriterConfig(analyzer);
//		config.setSimilarity(new BM25Similarity());	
        
        
		//BM25 with Porter stemmer and no stop words
//		analyzer = new EnglishAnalyzer(CharArraySet.EMPTY_SET);
//		config = new IndexWriterConfig(analyzer);
//		config.setSimilarity(new BM25Similarity());
		
		
		//BM25 with stop words and no stemmer
//		CharArraySet stopWords = StandardAnalyzer.STOP_WORDS_SET; // Standard analyzer : + stop words - stemmer
//		analyzer = new StandardAnalyzer(stopWords);
//		config = new IndexWriterConfig(analyzer);
//      config.setSimilarity(new BM25Similarity());		
		
		

		config.setOpenMode(OpenMode.CREATE);
		IndexWriter iwriter = new IndexWriter(directory, config);

		// Indexing : title, abstract, task number, relevance
        for (DocumentInCollection documentdoc : docs) {
            Document doc = new Document();
			doc.add(new Field("title", documentdoc.getTitle(), TextField.TYPE_STORED));
			doc.add(new Field("abstractText", documentdoc.getAbstractText(), TextField.TYPE_STORED));
			doc.add(new Field("search_task_number", Integer.toString(documentdoc.getSearchTaskNumber()), TextField.TYPE_STORED));
			doc.add(new Field("relevance", Boolean.toString(documentdoc.isRelevant()), TextField.TYPE_STORED));
            iwriter.addDocument(doc);
        }		
		
		iwriter.close();
		directory.close();
		
		return config;
	}
	
	public List<String> search(String searchQuery, IndexWriterConfig cf) throws IOException, ParseException {

		// Print search query
		System.out.print("searchQuery: " + searchQuery);
		System.out.println("");
		
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
		
		TopDocs docs = isearcher.search(booleanQuery.build(), 1000);
		ScoreDoc[] scored = docs.scoreDocs;
		
		// Save the results
		for (ScoreDoc aDoc : scored) {
			Document d = isearcher.doc(aDoc.doc);
			results.add("score: " + aDoc.score + " | relevance: " + d.get("relevance") + " | title: " + d.get("title") + " | abstractText: " + d.get("abstractText"));
		}
		
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
			
			DocumentCollectionParser parser = new DocumentCollectionParser();
			parser.parse(args[0]);
			List<DocumentInCollection> docs = parser.getDocuments();
				
			IndexWriterConfig cf = engine.index(docs);

			List<String> results;
			
			// testing query for stemming and stopwords (test with => corpus_part2_test.xml)
			results = engine.search("the speech recognitions", cf);
			
			// Display the results
			engine.printResults(results);
			
		}
		else
			System.out.println("ERROR: the path of a document file has to be passed as a command line argument.");
	}
}
