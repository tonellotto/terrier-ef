package it.cnr.isti.hpclab;

import java.io.IOException;

import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;

public class QuasiSuccintIndexTest 
{
	public static void main(String[] args) throws IOException
	{
		Index index = Index.createIndex("/Users/khast/index-java", "cacca");
		
		// final int numDocs = index.getCollectionStatistics().getNumberOfDocuments();
		/*
		DocumentIndex docIndex = index.getDocumentIndex();
		
		for (int i = 0; i < numDocs; i++)
			System.err.println(docIndex.getDocumentLength(i));
		*/
		Lexicon<String> lex = index.getLexicon();
		
		LexiconEntry le = lex.getLexiconEntry("attori");
		System.err.println(le);
		
		IterablePosting p = index.getInvertedIndex().getPostings(le);
		while (p.next() != IterablePosting.END_OF_LIST)
		{
			System.err.println(p);
		}
	}
}
