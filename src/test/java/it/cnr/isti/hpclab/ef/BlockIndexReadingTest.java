/*
 * Elias-Fano compression for Terrier 5
 *
 * Copyright (C) 2018-2018 Nicola Tonellotto 
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */
package it.cnr.isti.hpclab.ef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

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
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.structures.postings.bit.BlockIterablePosting;
import org.terrier.utility.ApplicationSetup;

import it.cnr.isti.hpclab.ef.structures.EFLexiconEntry;
import it.cnr.isti.hpclab.ef.EliasFano;
import it.cnr.isti.hpclab.ef.Generator;
import it.cnr.isti.hpclab.ef.structures.EFBlockIterablePosting;

@RunWith(value = Parameterized.class)
public class BlockIndexReadingTest extends EFSetupTest
{
	protected IndexOnDisk originalIndex = null;
	protected IndexOnDisk efIndex = null;
	
	private int parallelism;
	private int skipSize;
	
	public BlockIndexReadingTest(int parallelism, int skipSize)
	{
		this.parallelism = parallelism;
		this.skipSize = skipSize;
	}
	
	@Parameters
	public static Collection<Object[]> skipSizeValues()
	{
		// return Arrays.asList(new Object[][] { {2, 2} });
		return Arrays.asList(new Object[][] { {1,2}, {1,3}, {1,4}, {2,2}, {2,3}, {2,4}, {3,2}, {3,3}, {3,4}});
	}
	
	@Before 
	public void createIndex() throws Exception
	{
		ApplicationSetup.BLOCK_INDEXING = true;
		// ApplicationSetup.MAX_BLOCKS = 2;
		super.doShakespeareIndexing();
		originalIndex = Index.createIndex();
		
		String args[] = {"-path", originalIndex.getPath(), "-prefix", originalIndex.getPrefix() + ".ef", "-index", originalIndex.getPath() + File.separator + originalIndex.getPrefix() + ".properties", "-p", Integer.toString(parallelism), "-b"};

		System.setProperty(EliasFano.LOG2QUANTUM, "3");

		Generator.main(args);
		
		efIndex = Index.createIndex(args[1], args[3]);
	}
	
	@Test(expected = Test.None.class /* no exception expected */)
	public void testRandomPostingLists() throws IOException
	{
		randomSanityCheck(originalIndex, efIndex);
	}
	
	@Test 
	public void testPostingLists() throws IOException
	{
		assertEquals(originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(), efIndex.getCollectionStatistics().getNumberOfUniqueTerms());
		
		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> efEntry;
		
		BasicLexiconEntry ble;
		EFLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			efEntry = efIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), efEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (EFLexiconEntry) efEntry.getValue();
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			BlockIterablePosting op = (BlockIterablePosting) originalIndex.getInvertedIndex().getPostings(ble);
			EFBlockIterablePosting sp = (EFBlockIterablePosting) efIndex.getInvertedIndex().getPostings(sle);
			
			while (op.next() != IterablePosting.EOL && sp.next() != IterablePosting.EOL) {
				assertEquals(op.getId(), sp.getId());
				assertEquals(op.getFrequency(), sp.getFrequency());
				assertEquals(op.getDocumentLength(), sp.getDocumentLength());
				assertArrayEquals(op.getPositions(), sp.getPositions());
			}
		}
	}

	@Test 
	public void testPostingListsLazyPositionsRead() throws IOException
	{
		assertEquals(originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(), efIndex.getCollectionStatistics().getNumberOfUniqueTerms());
		
		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> efEntry;
		
		BasicLexiconEntry ble;
		EFLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			efEntry = efIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), efEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (EFLexiconEntry) efEntry.getValue();
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			BlockIterablePosting op = (BlockIterablePosting) originalIndex.getInvertedIndex().getPostings(ble);
			EFBlockIterablePosting sp = (EFBlockIterablePosting) efIndex.getInvertedIndex().getPostings(sle);
			
			int cnt = 0;
			while (op.next() != IterablePosting.EOL && sp.next() != IterablePosting.EOL) {
				assertEquals(op.getId(), sp.getId());
				assertEquals(op.getFrequency(), sp.getFrequency());
				assertEquals(op.getDocumentLength(), sp.getDocumentLength());
				if (cnt++ % 2 == 0)
					assertArrayEquals(op.getPositions(), sp.getPositions());
			}
		}
	}

	@Test 
	public void testPostingListsRepeatPositionsRead() throws IOException
	{
		assertEquals(originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(), efIndex.getCollectionStatistics().getNumberOfUniqueTerms());
		
		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> efEntry;
		
		BasicLexiconEntry ble;
		EFLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			efEntry = efIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), efEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (EFLexiconEntry) efEntry.getValue();
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			BlockIterablePosting op = (BlockIterablePosting) originalIndex.getInvertedIndex().getPostings(ble);
			EFBlockIterablePosting sp = (EFBlockIterablePosting) efIndex.getInvertedIndex().getPostings(sle);
			
			while (op.next() != IterablePosting.EOL && sp.next() != IterablePosting.EOL) {
				assertEquals(op.getId(), sp.getId());
				assertEquals(op.getFrequency(), sp.getFrequency());
				assertEquals(op.getDocumentLength(), sp.getDocumentLength());
				assertArrayEquals(op.getPositions(), sp.getPositions());
				assertArrayEquals(op.getPositions(), sp.getPositions());
			}
		}
	}

	@Test
	public void nextIntoEverySkip() throws IOException
	{
		//System.err.println("Skipping every " + skipSize + " postings");
		
		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> efEntry;
		
		BasicLexiconEntry ble;
		EFLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			efEntry = efIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), efEntry.getKey());
			//System.err.println(efEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (EFLexiconEntry) efEntry.getValue();
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			BlockIterablePosting op = (BlockIterablePosting) originalIndex.getInvertedIndex().getPostings(ble);
			EFBlockIterablePosting sp = (EFBlockIterablePosting) efIndex.getInvertedIndex().getPostings(sle);
			
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
					assertArrayEquals(op.getPositions(), sp.getPositions());
				}			
			}
		}
	}
	

	@Test
	public void nextAfterEverySkip() throws IOException
	{
		//System.err.println("Skipping after every " + skipSize + " postings");
		
		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> efEntry;
		
		BasicLexiconEntry ble;
		EFLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			efEntry = efIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), efEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (EFLexiconEntry) efEntry.getValue();
			
			//System.err.println(efEntry.getKey() + " has " + ble.getDocumentFrequency() + " postings");
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			BlockIterablePosting op = (BlockIterablePosting) originalIndex.getInvertedIndex().getPostings(ble);
			EFBlockIterablePosting sp = (EFBlockIterablePosting) efIndex.getInvertedIndex().getPostings(sle);
			
			// int numSkips = 0;
			
			int cnt = 0;
			while (op.next() != IterablePosting.EOL) {
				
				if (++cnt == skipSize) {
					cnt = 0;
					// numSkips++;
					sp.next(op.getId() + 1);
					op.next();
					//System.err.println(op.getId());
					//System.err.println(sp.getId());
					assertEquals(op.getId(), sp.getId());
					if (op.getId() != IterablePosting.EOL) {
						assertEquals(op.getFrequency(), sp.getFrequency());
						assertEquals(op.getDocumentLength(), sp.getDocumentLength());
						assertArrayEquals(op.getPositions(), sp.getPositions());

					}
				}			
			}
			/*
			if (numSkips > 0)
				System.err.println("SKIP: " + numSkips);
				*/
		}
	}
	
	@After public void deleteIndex() throws IOException
	{
		originalIndex.close();
		efIndex.close();
	}
	
	public static void randomSanityCheck(IndexOnDisk srcIndex, IndexOnDisk dstIndex) throws IOException 
	{
		Random rnd = new Random(System.currentTimeMillis());
		int numSamples = 50 + 1 + rnd.nextInt(50);
		System.err.println("Randomly checking " + numSamples + " terms and posting list for sanity check");
		
		assertEquals("Original index has " + srcIndex.getCollectionStatistics().getNumberOfUniqueTerms() + " unique terms\n" + 
					 "Elias-Fano index has " + dstIndex.getCollectionStatistics().getNumberOfUniqueTerms() + " unique terms", 
				     srcIndex.getCollectionStatistics().getNumberOfUniqueTerms(), dstIndex.getCollectionStatistics().getNumberOfUniqueTerms());

		/*
		if (srcIndex.getCollectionStatistics().getNumberOfUniqueTerms() != dstIndex.getCollectionStatistics().getNumberOfUniqueTerms()) {
			System.err.println("Original  index has " + srcIndex.getCollectionStatistics().getNumberOfUniqueTerms() + " unique terms");
			System.err.println("EliasFano index has " + dstIndex.getCollectionStatistics().getNumberOfUniqueTerms() + " unique terms");
			System.exit(-1);
		}
		*/

		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> efEntry;
		
		BasicLexiconEntry ble;
		EFLexiconEntry efle;

		for (int i = 0; i < numSamples; i++) {
			int termid = rnd.nextInt(dstIndex.getCollectionStatistics().getNumberOfUniqueTerms());
			
			originalEntry = srcIndex.getLexicon().getLexiconEntry(termid);
			efEntry       = dstIndex.getLexicon().getLexiconEntry(termid);
						
			ble  = (BasicLexiconEntry) originalEntry.getValue();
			efle = (EFLexiconEntry) efEntry.getValue();

			System.err.println("Checking term " + originalEntry.getKey() + " (" + originalEntry.getValue().getDocumentFrequency() + " entries), termid " + termid + " Tot pos " + originalEntry.getValue().getFrequency());
			IterablePosting srcPosting = srcIndex.getInvertedIndex().getPostings(ble);
			EFBlockIterablePosting dstPosting = (EFBlockIterablePosting) dstIndex.getInvertedIndex().getPostings(efle);
						
			while (srcPosting.next() != IterablePosting.END_OF_LIST && dstPosting.next() != IterablePosting.END_OF_LIST) {
				/*
				if ((srcPosting.getId() != dstPosting.getId()) || (srcPosting.getFrequency() != dstPosting.getFrequency())) {
					System.err.println("Something went wrong in random sanity check...");
					System.exit(-1);
				}
				*/
				assertEquals("Something went wrong in random sanity check...", srcPosting.getId(), dstPosting.getId());
				assertEquals("Something went wrong in random sanity check...", srcPosting.getFrequency(), dstPosting.getFrequency());
				
				int[] srcPositions = ((BlockPosting)srcPosting).getPositions();
				int[] dstPositions = dstPosting.getPositions();
				
				/*
				if (srcPositions.length != dstPositions.length) {
					System.err.println("Something went wrong in random sanity check...");
					throw new IllegalStateException();
				}
				*/
				
				assertEquals("Something went wrong in random sanity check...", srcPositions.length, dstPositions.length);
				
				for (int j = 0; j < srcPositions.length; ++j) {
//					System.err.println("src " + srcPositions[j] + "\tdst " + dstPositions[j]);
					/*
					if (srcPositions[j] != dstPositions[j]) {
						System.err.println("Something went wrong in random sanity check...");
						throw new IllegalStateException();
					}
					*/
					assertEquals("Something went wrong in random sanity check...", srcPositions[j], dstPositions[j]);
				}
			}
		}
	}

}
