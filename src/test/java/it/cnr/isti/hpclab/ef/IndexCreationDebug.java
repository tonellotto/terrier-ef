package it.cnr.isti.hpclab.ef;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
// import org.junit.Test;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;

public class IndexCreationDebug extends ApplicationSetupTest
{
	private Index index = null;
	
	@Before public void createIndex() throws Exception
	{
		super.doShakespeareIndexing();
		index = Index.createIndex();
		
	}
	
	@After public void deleteIndex() throws IOException
	{
		index.close();
	} 
	
	//@Test 
	public void printPostingList() throws IOException
	{
		Lexicon<String> lex = index.getLexicon();
		
		LexiconEntry le = lex.getLexiconEntry("new");
		System.err.println(le);
		
		IterablePosting p = index.getInvertedIndex().getPostings(le);
		while (p.next() != IterablePosting.EOL)
		{
			System.err.print(p + ", ");
		}
		System.err.println();
	}
	
}
