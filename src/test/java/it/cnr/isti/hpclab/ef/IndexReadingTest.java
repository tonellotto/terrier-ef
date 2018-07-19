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

import it.cnr.isti.hpclab.ef.structures.EFLexiconEntry;

@RunWith(value = Parameterized.class)
public class IndexReadingTest extends ApplicationSetupTest
{
	protected IndexOnDisk originalIndex = null;
	protected IndexOnDisk succinctIndex = null;
	
	private int skipSize;
	
	public IndexReadingTest(int skipSize)
	{
		this.skipSize = skipSize;
	}
	
	@Parameters
	public static Collection<Object[]> skipSizeValues()
	{
		return Arrays.asList(new Object[][] { {2} });
		//return Arrays.asList(new Object[][] { {2}, {3}, {4} });
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
		
		succinctIndex = Index.createIndex(args[0], args[1] + EliasFano.USUAL_EXTENSION);
		// System.out.println(succinctIndex.getIndexProperty("log2Quantum", ""));
	}
	
	@Test
	public void testRandomPostingLists() throws IOException
	{
		Generator.randomSanityCheck(originalIndex, succinctIndex);
	}
	
	@Test 
	public void testPostingLists() throws IOException
	{
		assertEquals(originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(), succinctIndex.getCollectionStatistics().getNumberOfUniqueTerms());
		
		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> succinctEntry;
		
		BasicLexiconEntry ble;
		EFLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			succinctEntry = succinctIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), succinctEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (EFLexiconEntry) succinctEntry.getValue();
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			IterablePosting op = originalIndex.getInvertedIndex().getPostings(ble);
			IterablePosting sp = succinctIndex.getInvertedIndex().getPostings(sle);
			
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
		EFLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			succinctEntry = succinctIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), succinctEntry.getKey());
			//System.err.println(succinctEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (EFLexiconEntry) succinctEntry.getValue();
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			IterablePosting op = originalIndex.getInvertedIndex().getPostings(ble);
			IterablePosting sp = succinctIndex.getInvertedIndex().getPostings(sle);
			
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
		Map.Entry<String, LexiconEntry> succinctEntry;
		
		BasicLexiconEntry ble;
		EFLexiconEntry sle;
		
		for (int i = 0; i < originalIndex.getCollectionStatistics().getNumberOfUniqueTerms(); i++) {
			originalEntry = originalIndex.getLexicon().getIthLexiconEntry(i);
			succinctEntry = succinctIndex.getLexicon().getIthLexiconEntry(i);
			
			assertEquals(originalEntry.getKey(), succinctEntry.getKey());
			
			ble = (BasicLexiconEntry) originalEntry.getValue();
			sle = (EFLexiconEntry) succinctEntry.getValue();
			
			// System.err.println(succinctEntry.getKey() + " has " + ble.getDocumentFrequency() + " postings");
			
			assertEquals(ble.getDocumentFrequency(), sle.getDocumentFrequency());
			
			IterablePosting op = originalIndex.getInvertedIndex().getPostings(ble);
			IterablePosting sp = succinctIndex.getInvertedIndex().getPostings(sle);
			
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
		succinctIndex.close();
	} 
}
