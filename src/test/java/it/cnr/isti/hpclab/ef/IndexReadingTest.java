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

import java.io.File;
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

import it.cnr.isti.hpclab.ef.EliasFano;
import it.cnr.isti.hpclab.ef.Generator;
import it.cnr.isti.hpclab.ef.structures.EFLexiconEntry;

@RunWith(value = Parameterized.class)
public class IndexReadingTest extends ApplicationSetupTest
{
	protected IndexOnDisk originalIndex = null;
	protected IndexOnDisk efIndex = null;
	
	private int parallelism;
	private int skipSize;
	
	public IndexReadingTest(int parallelism, int skipSize)
	{
		this.parallelism = parallelism;
		this.skipSize = skipSize;
	}
	
	@Parameters
	public static Collection<Object[]> getParameters()
	{
		// return Arrays.asList(new Object[][] { {3, 2} });
		return Arrays.asList(new Object[][] { {1,2}, {1,3}, {1,4}, {2,2}, {2,3}, {2,4}, {3,2}, {3,3}, {3,4}});
	}
	
	@Before 
	public void createIndex() throws Exception
	{
		super.doShakespeareIndexing();
		originalIndex = Index.createIndex();
		
		String args[] = {"-path", originalIndex.getPath(), "-prefix", originalIndex.getPrefix() + ".ef", "-index", originalIndex.getPath() + File.separator + originalIndex.getPrefix() + ".properties", "-p", Integer.toString(parallelism)};

		System.setProperty(EliasFano.LOG2QUANTUM, "3");

		Generator.main(args);
		
		efIndex = Index.createIndex(args[1], args[3]);
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testRandomPostingLists() throws IOException
	{
		it.cnr.isti.hpclab.ef.OldGenerator.randomSanityCheck(originalIndex, efIndex);
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
			
			IterablePosting op = originalIndex.getInvertedIndex().getPostings(ble);
			IterablePosting sp = efIndex.getInvertedIndex().getPostings(sle);
			
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
			
			IterablePosting op = originalIndex.getInvertedIndex().getPostings(ble);
			IterablePosting sp = efIndex.getInvertedIndex().getPostings(sle);
			
			// int numSkips = 0;
			
			int cnt = 0;
			while (op.next() != IterablePosting.EOL) {
				
				if (++cnt == skipSize) {
					cnt = 0;
					// numSkips++;
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
		Map.Entry<String, LexiconEntry> efEntry;
		
		BasicLexiconEntry ble;
		EFLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			efEntry = efIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), efEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (EFLexiconEntry) efEntry.getValue();
			
			// System.err.println(efEntry.getKey() + " has " + ble.getDocumentFrequency() + " postings");
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			IterablePosting op = originalIndex.getInvertedIndex().getPostings(ble);
			IterablePosting sp = efIndex.getInvertedIndex().getPostings(sle);
			
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
	
	@After 
	public void deleteIndex() throws IOException
	{
		originalIndex.close();
		efIndex.close();
	} 
}