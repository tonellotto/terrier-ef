package it.cnr.isti.hpclab;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.terrier.structures.BasicLexiconEntry;
import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;

import it.cnr.isti.hpclab.ef.ApplicationSetupTest;
import it.cnr.isti.hpclab.ef.EliasFano;
import it.cnr.isti.hpclab.ef.Generator;
import it.cnr.isti.hpclab.structures.VarintLexiconEntry;

@RunWith(value = Parameterized.class)
public class VarintLexiconGeneratorTest extends ApplicationSetupTest
{
	protected IndexOnDisk originalIndex = null;
	protected IndexOnDisk efIndex = null;
	protected IndexOnDisk varintIndex = null;
	
	private int skipSize;
	
	public VarintLexiconGeneratorTest(int skipSize)
	{
		this.skipSize = skipSize;
	}
	
	@Parameters
	public static Collection<Object[]> skipSizeValues()
	{
		return Arrays.asList(new Object[][] { {2}, {3}, {4} });
	}
	
	@Before 
	public void createIndex() throws Exception
	{
		super.doShakespeareIndexing();
		originalIndex = Index.createIndex();
		
		String args[] = new String[2];
		args[0] = originalIndex.getPath();
		args[1] = originalIndex.getPrefix();
		Generator.LOG2QUANTUM = 3;
		Generator.main(args);
		
		efIndex = Index.createIndex(args[0], args[1] + EliasFano.USUAL_EXTENSION);
		
		args = new String[4];
		args[0] = "-d";
		args[1] = efIndex.getPath();
		args[2] = "-i";
		args[3] = efIndex.getPrefix();
		
		VarintLexiconGenerator.main(args);
		
		varintIndex = Index.createIndex(args[1], args[3]);
		VarintLexiconGenerator.overwriteVarintLexiconProperties(varintIndex);
		varintIndex.close();
		
		// Reopen to reload properties
		varintIndex = Index.createIndex(args[1], args[3]);
	}
	
	@Test
	public void testRandomPostingLists() throws IOException
	{
		Generator.randomSanityCheck(originalIndex, varintIndex);
	}
	
	@Test 
	public void testPostingLists() throws IOException
	{
		assertEquals(originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(), varintIndex.getCollectionStatistics().getNumberOfUniqueTerms());
		
		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> succinctEntry;
		
		BasicLexiconEntry ble;
		VarintLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			succinctEntry = varintIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), succinctEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (VarintLexiconEntry) succinctEntry.getValue();
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			IterablePosting op = originalIndex.getInvertedIndex().getPostings(ble);
			IterablePosting sp = varintIndex.getInvertedIndex().getPostings(sle);
			
			while (op.next() != IterablePosting.EOL && sp.next() != IterablePosting.EOL) {
				assertEquals(op.getId(), sp.getId());
				assertEquals(op.getFrequency(), sp.getFrequency());
				assertEquals(op.getDocumentLength(), sp.getDocumentLength());
			}
		}
	}
	
	@Test
	public void nextIntoEverySkip() throws IOException
	{
		// System.err.println("Skipping every " + skipSize + " postings");
		
		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> succinctEntry;
		
		BasicLexiconEntry ble;
		VarintLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			succinctEntry = varintIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), succinctEntry.getKey());
			//System.err.println(succinctEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (VarintLexiconEntry) succinctEntry.getValue();
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			IterablePosting op = originalIndex.getInvertedIndex().getPostings(ble);
			IterablePosting sp = varintIndex.getInvertedIndex().getPostings(sle);
			
//			int numSkips = 0;
			
			int cnt = 0;
			while (op.next() != IterablePosting.EOL) {
				
				if (++cnt == skipSize) {
					cnt = 0;
//					numSkips++;
					sp.next(op.getId());
					assertTrue(sp.getId() != IterablePosting.EOL);
					assertEquals(op.getId(), sp.getId());
					assertEquals(op.getFrequency(), sp.getFrequency());
					assertEquals(op.getDocumentLength(), sp.getDocumentLength());
				}			
			}

			// if (numSkips > 0)
			//	System.err.println("SKIP: " + numSkips);
		}
	}
	

	@Test
	public void nextAfterEverySkip() throws IOException
	{
		// System.err.println("Skipping after every " + skipSize + " postings");
		
		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> succinctEntry;
		
		BasicLexiconEntry ble;
		VarintLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			succinctEntry = varintIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), succinctEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (VarintLexiconEntry) succinctEntry.getValue();
			
			// System.err.println(succinctEntry.getKey() + " has " + ble.getDocumentFrequency() + " postings");
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			IterablePosting op = originalIndex.getInvertedIndex().getPostings(ble);
			IterablePosting sp = varintIndex.getInvertedIndex().getPostings(sle);
			
			// int numSkips = 0;
			
			int cnt = 0;
			while (op.next() != IterablePosting.EOL) {
				
				if (++cnt == skipSize) {
					cnt = 0;
					// numSkips++;
					sp.next(op.getId() + 1);
					op.next();
					// System.err.println(op.getId());
					assertEquals(op.getId(), sp.getId());
					if (op.getId() != IterablePosting.EOL) {
						assertEquals(op.getFrequency(), sp.getFrequency());
						assertEquals(op.getDocumentLength(), sp.getDocumentLength());
					}
				}			
			}
		}
	}
	
	@After public void deleteIndex() throws IOException
	{
		originalIndex.close();
		efIndex.close();
		varintIndex.close();
	} 
}