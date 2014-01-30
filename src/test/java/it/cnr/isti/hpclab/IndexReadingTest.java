package it.cnr.isti.hpclab;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.terrier.structures.Index;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.utility.ApplicationSetup;

public class IndexReadingTest 
{
	protected static final String indexPath = "/Users/khast/index-java/";
	protected static final String indexPrefix = "cw09b";
	protected static final String term = "new"; // for wt10g
	protected static final String term2 = "000"; // for wt10g
	protected static final String term3 = "attorno"; // for wt10g
	protected static final String term4 = "attori"; // for wt10g
	protected static Index index = null;
	
	protected static void setIndex(String path, String prefix) throws IOException
	{
		if (index != null)
			index.close();
		
		index = Index.createIndex(path, prefix);
		if (Index.getLastIndexLoadError() != null) 
			System.err.println(Index.getLastIndexLoadError());
	}
	
	
	@Test
	public void testDocids() throws IOException
	{
		ApplicationSetup.setProperty("stopwords.filename", System.getProperty("user.dir") + File.separator + "src/main/resources/stopword-list.txt");
		ApplicationSetup.setProperty("terrier.properties", System.getProperty("user.dir") + File.separator + "src/main/resources/terrier.properties");

		setIndex(indexPath, indexPrefix);

		int[] correctDocids = null;
		int[] correctFreqs = null;
		
		LexiconEntry le = index.getLexicon().getLexiconEntry(term);
		correctDocids = new int[le.getDocumentFrequency()];
		correctFreqs  = new int[le.getDocumentFrequency()];
		
		IterablePosting p = index.getInvertedIndex().getPostings(le);
		int i = 0;
		while (p.next() != IterablePosting.END_OF_LIST) {
			correctDocids[i] = p.getId();
			correctFreqs[i] = p.getFrequency();
			i++;
		}
		
		System.out.println("Found " + correctDocids.length + " docids in original index");
		
		setIndex(indexPath, indexPrefix + ".succinct");
		le = index.getLexicon().getLexiconEntry(term);
		
		assertEquals(le.getDocumentFrequency(), correctDocids.length);
		
		int[] foundDocids = new int[le.getDocumentFrequency()];
		int[] foundFreqs  = new int[le.getDocumentFrequency()];
		
		System.out.println("Found " + foundDocids.length + " docids in quasi succinct index");
		
		p = index.getInvertedIndex().getPostings(le);
		i = 0;
		while (p.next() != IterablePosting.END_OF_LIST) {
			foundDocids[i] = p.getId();
			foundFreqs[i] = p.getFrequency();
			i++;
		}
		
		for (i = 0; i < correctDocids.length; i++) {
			assertEquals(correctDocids[i], foundDocids[i]);
			assertEquals(correctFreqs[i], foundFreqs[i]);
		}
	}
	
	@Test
	public void testSkip25() throws IOException
	{
		setIndex(indexPath, indexPrefix);
		LexiconEntry le = index.getLexicon().getLexiconEntry(term);
		IterablePosting p = index.getInvertedIndex().getPostings(le);
		
		int i;
		int skipDocid = IterablePosting.END_OF_LIST;
		for (i = 0; i < le.getDocumentFrequency() / 4; i++)
			skipDocid = p.next(); 
		
		int[] foundDocids = new int[le.getDocumentFrequency() - i + 1];
		int[] correctDocids = new int[le.getDocumentFrequency() - i + 1];
		
		int[] foundFreqs = new int[le.getDocumentFrequency() - i + 1];
		int[] correctFreqs = new int[le.getDocumentFrequency() - i + 1];
		
		p = index.getInvertedIndex().getPostings(le);
		i = 0;
		p.next(skipDocid);
		do {
			correctDocids[i] = p.getId();
			correctFreqs[i] = p.getFrequency();
			i++;
		} while (p.next() != IterablePosting.END_OF_LIST);
			
		System.out.println("Found " + correctDocids.length + " docids in original index");
		
		setIndex(indexPath, indexPrefix + ".succinct");
		le = index.getLexicon().getLexiconEntry(term);
		
		System.out.println("Found " + correctDocids.length + " docids in quasi succinct index");
		
		p = index.getInvertedIndex().getPostings(le);
		i = 0;
		p.next(skipDocid);
		do {
			foundDocids[i] = p.getId();
			foundFreqs[i] = p.getFrequency();
			i++;
		} while (p.next() != IterablePosting.END_OF_LIST);
		
		for (i = 0; i < correctDocids.length; i++) {
			assertEquals(correctDocids[i], foundDocids[i]);
			assertEquals(correctFreqs[i], foundFreqs[i]);
		}
	}

	@Test
	public void testSkip50() throws IOException
	{
		setIndex(indexPath, indexPrefix);
		LexiconEntry le = index.getLexicon().getLexiconEntry(term);
		IterablePosting p = index.getInvertedIndex().getPostings(le);
		
		int i;
		int skipDocid = IterablePosting.END_OF_LIST;
		for (i = 0; i < le.getDocumentFrequency() / 4 * 2; i++)
			skipDocid = p.next(); 
		
		int[] foundDocids = new int[le.getDocumentFrequency() - i + 1];
		int[] correctDocids = new int[le.getDocumentFrequency() - i + 1];
		
		int[] foundFreqs = new int[le.getDocumentFrequency() - i + 1];
		int[] correctFreqs = new int[le.getDocumentFrequency() - i + 1];
		
		p = index.getInvertedIndex().getPostings(le);
		i = 0;
		p.next(skipDocid);
		do {
			correctDocids[i] = p.getId();
			correctFreqs[i] = p.getFrequency();
			i++;
		} while (p.next() != IterablePosting.END_OF_LIST);
			
		System.out.println("Found " + correctDocids.length + " docids in original index");
		
		setIndex(indexPath, indexPrefix + ".succinct");
		le = index.getLexicon().getLexiconEntry(term);
		
		System.out.println("Found " + correctDocids.length + " docids in quasi succinct index");
		
		p = index.getInvertedIndex().getPostings(le);
		i = 0;
		p.next(skipDocid);
		do {
			foundDocids[i] = p.getId();
			foundFreqs[i] = p.getFrequency();
			i++;
		} while (p.next() != IterablePosting.END_OF_LIST);
		
		for (i = 0; i < correctDocids.length; i++) {
			assertEquals(correctDocids[i], foundDocids[i]);
			assertEquals(correctFreqs[i], foundFreqs[i]);
		}
	}
	
	@Test
	public void testSkip75() throws IOException
	{
		setIndex(indexPath, indexPrefix);
		LexiconEntry le = index.getLexicon().getLexiconEntry(term);
		IterablePosting p = index.getInvertedIndex().getPostings(le);
		
		int i;
		int skipDocid = IterablePosting.END_OF_LIST;
		for (i = 0; i < le.getDocumentFrequency() / 4 * 3; i++)
			skipDocid = p.next(); 
		
		int[] foundDocids = new int[le.getDocumentFrequency() - i + 1];
		int[] correctDocids = new int[le.getDocumentFrequency() - i + 1];
		
		int[] foundFreqs = new int[le.getDocumentFrequency() - i + 1];
		int[] correctFreqs = new int[le.getDocumentFrequency() - i + 1];
		
		p = index.getInvertedIndex().getPostings(le);
		i = 0;
		p.next(skipDocid);
		do {
			correctDocids[i] = p.getId();
			correctFreqs[i] = p.getFrequency();
			i++;
		} while (p.next() != IterablePosting.END_OF_LIST);
			
		System.out.println("Found " + correctDocids.length + " docids in original index");
		
		setIndex(indexPath,indexPrefix + ".succinct");
		le = index.getLexicon().getLexiconEntry(term);
		
		System.out.println("Found " + correctDocids.length + " docids in quasi succinct index");
		
		p = index.getInvertedIndex().getPostings(le);
		i = 0;
		p.next(skipDocid);
		do {
			foundDocids[i] = p.getId();
			foundFreqs[i] = p.getFrequency();
			i++;
		} while (p.next() != IterablePosting.END_OF_LIST);
		
		for (i = 0; i < correctDocids.length; i++) {
			assertEquals(correctDocids[i], foundDocids[i]);
			assertEquals(correctFreqs[i], foundFreqs[i]);
		}
	}
}
