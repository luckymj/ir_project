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
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.*;
import org.apache.lucene.util.BytesRef;


public class LuceneSearchApp_601234 {
	
	public LuceneSearchApp_601234() {

	}
	
	public IndexWriterConfig index(List<DocumentInCollection> docs) throws IOException {

		// implement the Lucene indexing here
		
		Analyzer analyzer = new StandardAnalyzer();
		
		// Store the index in memory OR on disk:
		//Directory directory = new RAMDirectory();
		Directory directory = FSDirectory.open(Paths.get("index"));

		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		

		//VSM + stopwords + stemmer
		CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet(); //EnglishAnalyzer already has a stemmer
		analyzer = new EnglishAnalyzer(stopWords);
        config = new IndexWriterConfig(analyzer);
        config.setSimilarity(new ClassicSimilarity());

		
		// Standard analyzer : stop words - porter stemmer
		

		config.setOpenMode(OpenMode.CREATE);
		IndexWriter iwriter = new IndexWriter(directory, config);

        int doccount = 0;
        for (DocumentInCollection documentdoc : docs) {
            Document doc = new Document();
			doc.add(new Field("title", documentdoc.getTitle(), TextField.TYPE_STORED));
			doc.add(new Field("abstractText", documentdoc.getAbstractText(), TextField.TYPE_STORED));
			doc.add(new Field("search_task_number", Integer.toString(documentdoc.getSearchTaskNumber()), TextField.TYPE_STORED));
            iwriter.addDocument(doc);
            doccount++;
        }		
		
		iwriter.close();
		directory.close();
		
		return config;
	}
	
	public List<String> search(String searchQuery, IndexWriterConfig cf) throws IOException, ParseException {

		System.out.print("searchQuery: " + searchQuery);
		System.out.println("");
		
		QueryParser qp = new QueryParser("abstractText", cf.getAnalyzer());
		Query stemmedQuery = null;
		try {
			stemmedQuery = qp.parse(searchQuery);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
		
		// search only documents with task number 9
		booleanQuery.add(new TermQuery(new Term("search_task_number", "9")), BooleanClause.Occur.MUST);
		booleanQuery.add(stemmedQuery, BooleanClause.Occur.MUST);
		
		System.out.println(stemmedQuery.getClass().getSimpleName());
		
		List<String> results = new LinkedList<String>();

		// implement the Lucene search here

		DirectoryReader ireader = DirectoryReader.open(FSDirectory.open(Paths.get("index")));
		// System.out.println("Docs: " + ireader.getDocCount("Title"));
		IndexSearcher isearcher = new IndexSearcher(ireader);
		
		isearcher.setSimilarity(cf.getSimilarity());
		
		TopDocs docs = isearcher.search(booleanQuery.build(), 10);
		ScoreDoc[] scored = docs.scoreDocs;
		
		for (ScoreDoc aDoc : scored) {
			Document d = isearcher.doc(aDoc.doc);
			results.add("score: " + aDoc.score + " | title: " + d.get("title") + " | abstractText: " + d.get("abstractText"));
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
			LuceneSearchApp_601234 engine = new LuceneSearchApp_601234();
			
			DocumentCollectionParser parser = new DocumentCollectionParser();
			parser.parse(args[0]);
			List<DocumentInCollection> docs = parser.getDocuments();
				
			IndexWriterConfig cf = engine.index(docs);

			List<String> results;
			
		
			// 1) search documents with words "kim" and "korea" in the title
			results = engine.search("speech", cf);
			engine.printResults(results);
			

		}
		else
			System.out.println("ERROR: the path of a document file has to be passed as a command line argument.");
	}
}
