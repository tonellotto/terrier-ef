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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;

import it.cnr.isti.hpclab.ef.structures.EFBasicIterablePosting;
import it.cnr.isti.hpclab.ef.structures.EFLexiconEntry;

@Deprecated
@RunWith(value = Parameterized.class)
public class OldDocidReaderGetCurrentPosTest extends ApplicationSetupTest
{
	protected IndexOnDisk originalIndex = null;
	protected IndexOnDisk efIndex = null;
	
	private int skipSize;
	
	public OldDocidReaderGetCurrentPosTest(int skipSize)
	{
		this.skipSize = skipSize;
	}
	
	@Parameters(name = "{index}: jump size={0}")
	public static Collection<Object[]> skipSizeValues()
	{
		// return Arrays.asList(new Object[][] { {2} });
		return Arrays.asList(new Object[][] { {2}, {3}, {4}, {10}, {17}, {10000000} });
	}

	@Before 
	public void createIndex() throws Exception
	{
		super.doShakespeareIndexing();
		originalIndex = Index.createIndex();
		
		String args[] = new String[2];
		args[0] = originalIndex.getPath();
		args[1] = originalIndex.getPrefix();
		OldGenerator.LOG2QUANTUM = 3;
		OldGenerator.main(args);
		
		efIndex = Index.createIndex(args[0], args[1] + EliasFano.USUAL_EXTENSION);
		// System.out.println(efIndex.getIndexProperty("log2Quantum", ""));
	}
	
	@After 
	public void deleteIndex() throws IOException
	{
		originalIndex.close();
		efIndex.close();
	} 

	@Test
	public void testGetCurrentPos() throws IOException
	{
		Iterator<Entry<String, LexiconEntry>> lin = efIndex.getLexicon().iterator();
		
        Map.Entry<String, LexiconEntry> le = null;
        EFLexiconEntry le_in = null;

        int num_tests = 0;
        int docids[] = null;
        while (lin.hasNext()) {
        	le = lin.next();
        	le_in = (EFLexiconEntry) le.getValue();
        	if (le_in.getDocumentFrequency() >= 1) {
            	docids = new int[le_in.getDocumentFrequency()];
            	EFBasicIterablePosting p = (EFBasicIterablePosting) efIndex.getInvertedIndex().getPostings(le_in);
            	// First, we read all docids in an array
            	int pos = 0;
            	while (p.next() != IterablePosting.END_OF_LIST) {
            		docids[pos++] = p.getId();
            	}
            	p.close();
            	
            	// Second, we jump over the posting list and check
            	p = (EFBasicIterablePosting) efIndex.getInvertedIndex().getPostings(le_in);
            	
            	for (int i = 0; i < docids.length; i += skipSize) {
            		p.next(docids[i]);
            		assertEquals(i, p.getCurrentDocidPosition());
            	}
            	p.close();            	
            	num_tests++;
        	}
        }
        System.err.println("Tested " + num_tests + " posting lists");
	}
}
