package it.cnr.isti.hpclab;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import it.cnr.isti.hpclab.succinct.QuasiSuccinctIndexGenerator;
import it.cnr.isti.hpclab.succinct.structures.SuccinctLexiconEntry;

import java.io.IOException;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.terrier.structures.BasicLexiconEntry;
import org.terrier.structures.Index;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;

public class IndexReadingTest extends ApplicationSetupTest
{
	protected Index originalIndex = null;
	protected Index succinctIndex = null;
	
	@Before public void createIndex() throws Exception
	{
		super.doShakespeareIndexing();
		originalIndex = Index.createIndex();
		
		String args[] = new String[3];
		args[0] = originalIndex.getPath();
		args[1] = originalIndex.getPrefix();
		args[2] = originalIndex.getPrefix() + ".sux";
		QuasiSuccinctIndexGenerator.LOG2QUANTUM = 3;
		it.cnr.isti.hpclab.succinct.QuasiSuccinctIndexGenerator.main(args);
		
		succinctIndex = Index.createIndex(args[0], args[2]);
		// System.out.println(succinctIndex.getIndexProperty("log2Quantum", ""));
	}
	
	@Test public void testPostingLists() throws IOException
	{
		assertEquals(originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(), succinctIndex.getCollectionStatistics().getNumberOfUniqueTerms());
		
		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> succinctEntry;
		
		BasicLexiconEntry ble;
		SuccinctLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			succinctEntry = succinctIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), succinctEntry.getKey());
			//System.err.println(succinctEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (SuccinctLexiconEntry) succinctEntry.getValue();
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			IterablePosting op = originalIndex.getInvertedIndex().getPostings(ble);
			IterablePosting sp = succinctIndex.getInvertedIndex().getPostings(sle);
			
			while (op.next() != IterablePosting.END_OF_LIST && sp.next() != IterablePosting.END_OF_LIST) {
				assertEquals(op.getId(), sp.getId());
				assertEquals(op.getFrequency(), sp.getFrequency());
				assertEquals(op.getDocumentLength(), sp.getDocumentLength());
			}
		}
	}
	
	@Test
	public void nextIntoEvery2() throws IOException
	{
		System.err.println("Skipping every 2 postings");
		
		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> succinctEntry;
		
		BasicLexiconEntry ble;
		SuccinctLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			succinctEntry = succinctIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), succinctEntry.getKey());
			//System.err.println(succinctEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (SuccinctLexiconEntry) succinctEntry.getValue();
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			IterablePosting op = originalIndex.getInvertedIndex().getPostings(ble);
			IterablePosting sp = succinctIndex.getInvertedIndex().getPostings(sle);
			
			int numSkips = 0;
			
			int cnt = 0;
			while (op.next() != IterablePosting.END_OF_LIST) {
				
				if (++cnt == 2) {
					cnt = 0;
					numSkips++;
					sp.next(op.getId());
					assertTrue(sp.getId() != IterablePosting.END_OF_LIST);
					assertEquals(op.getId(), sp.getId());
					assertEquals(op.getFrequency(), sp.getFrequency());
					assertEquals(op.getDocumentLength(), sp.getDocumentLength());
				}			
			}

			if (numSkips > 0)
				System.err.println("SKIP: " + numSkips);
		}
	}
	
	@Test
	public void nextIntoEvery3() throws IOException
	{
		System.err.println("Skipping every 3 postings");
		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> succinctEntry;
		
		BasicLexiconEntry ble;
		SuccinctLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			succinctEntry = succinctIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), succinctEntry.getKey());
			//System.err.println(succinctEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (SuccinctLexiconEntry) succinctEntry.getValue();
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			IterablePosting op = originalIndex.getInvertedIndex().getPostings(ble);
			IterablePosting sp = succinctIndex.getInvertedIndex().getPostings(sle);
			
			int numSkips = 0;
			
			int cnt = 0;
			while (op.next() != IterablePosting.END_OF_LIST) {
				
				if (++cnt == 3) {
					cnt = 0;
					numSkips++;
					sp.next(op.getId());
					assertTrue(sp.getId() != IterablePosting.END_OF_LIST);
					assertEquals(op.getId(), sp.getId());
					assertEquals(op.getFrequency(), sp.getFrequency());
					assertEquals(op.getDocumentLength(), sp.getDocumentLength());
				}			
			}

			if (numSkips > 0)
				System.err.println("SKIP: " + numSkips);
		}
	}
	
	@Test
	public void nextIntoEvery4() throws IOException
	{
		System.err.println("Skipping every 4 postings");
		
		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> succinctEntry;
		
		BasicLexiconEntry ble;
		SuccinctLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			succinctEntry = succinctIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), succinctEntry.getKey());
			//System.err.println(succinctEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (SuccinctLexiconEntry) succinctEntry.getValue();
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			IterablePosting op = originalIndex.getInvertedIndex().getPostings(ble);
			IterablePosting sp = succinctIndex.getInvertedIndex().getPostings(sle);
			
			int numSkips = 0;
			
			int cnt = 0;
			while (op.next() != IterablePosting.END_OF_LIST) {
				
				if (++cnt == 4) {
					cnt = 0;
					numSkips++;
					sp.next(op.getId());
					assertTrue(sp.getId() != IterablePosting.END_OF_LIST);
					assertEquals(op.getId(), sp.getId());
					assertEquals(op.getFrequency(), sp.getFrequency());
					assertEquals(op.getDocumentLength(), sp.getDocumentLength());
				}			
			}

			if (numSkips > 0)
				System.err.println("SKIP: " + numSkips);
		}
	}
	
	@Test
	public void nextAfterEvery2() throws IOException
	{
		System.err.println("Skipping after every 2 postings");
		
		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> succinctEntry;
		
		BasicLexiconEntry ble;
		SuccinctLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			succinctEntry = succinctIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), succinctEntry.getKey());
			// System.err.println(succinctEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (SuccinctLexiconEntry) succinctEntry.getValue();
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			IterablePosting op = originalIndex.getInvertedIndex().getPostings(ble);
			IterablePosting sp = succinctIndex.getInvertedIndex().getPostings(sle);
			
			int numSkips = 0;
			
			int cnt = 0;
			while (op.next() != IterablePosting.END_OF_LIST) {
				
				if (++cnt == 2) {
					cnt = 0;
					numSkips++;
					sp.next(op.getId() + 1);
					op.next();
					assertEquals(op.getId(), sp.getId());
					if (op.getId() != IterablePosting.END_OF_LIST) {
						assertEquals(op.getFrequency(), sp.getFrequency());
						assertEquals(op.getDocumentLength(), sp.getDocumentLength());
					}
				}			
			}

			if (numSkips > 0)
				System.err.println("SKIP: " + numSkips);
		}
	}
	
	@Test
	public void nextAfterEvery3() throws IOException
	{
		System.err.println("Skipping after every 3 postings");
		
		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> succinctEntry;
		
		BasicLexiconEntry ble;
		SuccinctLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			succinctEntry = succinctIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), succinctEntry.getKey());
			// System.err.println(succinctEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (SuccinctLexiconEntry) succinctEntry.getValue();
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			IterablePosting op = originalIndex.getInvertedIndex().getPostings(ble);
			IterablePosting sp = succinctIndex.getInvertedIndex().getPostings(sle);
			
			int numSkips = 0;
			
			int cnt = 0;
			while (op.next() != IterablePosting.END_OF_LIST) {
				
				if (++cnt == 3) {
					cnt = 0;
					numSkips++;
					sp.next(op.getId() + 1);
					op.next();
					assertEquals(op.getId(), sp.getId());
					if (op.getId() != IterablePosting.END_OF_LIST) {
						assertEquals(op.getFrequency(), sp.getFrequency());
						assertEquals(op.getDocumentLength(), sp.getDocumentLength());
					}
				}			
			}

			if (numSkips > 0)
				System.err.println("SKIP: " + numSkips);
		}
	}
	
	@Test
	public void nextAfterEvery4() throws IOException
	{
		System.err.println("Skipping after every 4 postings");
		
		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> succinctEntry;
		
		BasicLexiconEntry ble;
		SuccinctLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			succinctEntry = succinctIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), succinctEntry.getKey());
			// System.err.println(succinctEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (SuccinctLexiconEntry) succinctEntry.getValue();
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			IterablePosting op = originalIndex.getInvertedIndex().getPostings(ble);
			IterablePosting sp = succinctIndex.getInvertedIndex().getPostings(sle);
			
			int numSkips = 0;
			
			int cnt = 0;
			while (op.next() != IterablePosting.END_OF_LIST) {
				
				if (++cnt == 4) {
					cnt = 0;
					numSkips++;
					sp.next(op.getId() + 1);
					op.next();
					assertEquals(op.getId(), sp.getId());
					if (op.getId() != IterablePosting.END_OF_LIST) {
						assertEquals(op.getFrequency(), sp.getFrequency());
						assertEquals(op.getDocumentLength(), sp.getDocumentLength());
					}
				}			
			}

			if (numSkips > 0)
				System.err.println("SKIP: " + numSkips);
		}
	}
	
	@After public void deleteIndex() throws IOException
	{
		originalIndex.close();
		succinctIndex.close();
	} 
}
