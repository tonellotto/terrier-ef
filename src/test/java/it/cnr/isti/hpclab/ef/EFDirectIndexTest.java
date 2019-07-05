/*
 * Elias-Fano compression for Terrier 5
 *
 * Copyright (C) 2018-2020 Nicola Tonellotto 
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

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.collections.FSArrayFile;
import org.terrier.structures.postings.IterablePosting;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import it.cnr.isti.hpclab.ef.structures.EFDirectIndex;
import it.cnr.isti.hpclab.ef.structures.EFInvertedIndex;

public class EFDirectIndexTest extends EFSetupTest
{
	protected IndexOnDisk originalIndex = null;
	protected IndexOnDisk efIndex = null;

	@Before 
	public void createIndex() throws Exception
	{
		System.setProperty("inverted2direct.processtokens", "1000");
		super.doShakespeareIndexing();
		originalIndex = Index.createIndex();
		
		String[] args = {"-path", originalIndex.getPath(), "-prefix", originalIndex.getPrefix() + ".ef", "-index", originalIndex.getPath() + File.separator + originalIndex.getPrefix() + ".properties", "-p", Integer.toString(1)};

		System.setProperty(EliasFano.LOG2QUANTUM, "3");

		Generator.main(args);
		
		efIndex = Index.createIndex(args[1], args[3]);
		
		String[] args1 = {"-index",  originalIndex.getPath() + File.separator + originalIndex.getPrefix() + ".properties"};
		Invert2Direct.main(args1);
		String[] args2 = {"-index",  efIndex.getPath() + File.separator + efIndex.getPrefix() + ".properties"};
		Invert2Direct.main(args2);
		
		// to save the new properties
		originalIndex.close();
		efIndex.close();
		
		// to load the new properties
		originalIndex = Index.createIndex();
		efIndex = Index.createIndex(args[1], args[3]);
	}
	
	@After
	public void closeIndex() throws IOException
	{
		originalIndex.close();
		efIndex.close();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testMD5hashcode() throws IOException
	{
		HashCode hc_or;
		HashCode hc_ef;

		hc_or = Files.asByteSource(new File(originalIndex.getPath() + File.separator + originalIndex.getPrefix() + ".direct" + FSArrayFile.USUAL_EXTENSION)).hash(Hashing.md5());
		hc_ef = Files.asByteSource(new File(efIndex.getPath() + File.separator + efIndex.getPrefix() + ".direct" + FSArrayFile.USUAL_EXTENSION)).hash(Hashing.md5());
		assertEquals(hc_or, hc_ef);
		
		hc_or = Files.asByteSource(new File(originalIndex.getPath() + File.separator + originalIndex.getPrefix() + ".direct" + EliasFano.DOCID_EXTENSION)).hash(Hashing.md5());
		hc_ef = Files.asByteSource(new File(efIndex.getPath() + File.separator + efIndex.getPrefix() + ".direct" + EliasFano.DOCID_EXTENSION)).hash(Hashing.md5());
		assertEquals(hc_or, hc_ef);

		hc_or = Files.asByteSource(new File(originalIndex.getPath() + File.separator + originalIndex.getPrefix() + ".direct" + EliasFano.FREQ_EXTENSION)).hash(Hashing.md5());
		hc_ef = Files.asByteSource(new File(efIndex.getPath() + File.separator + efIndex.getPrefix() + ".direct" + EliasFano.FREQ_EXTENSION)).hash(Hashing.md5());
		assertEquals(hc_or, hc_ef);
	}
	
	@Test
	public void testInv2DirContents() throws IOException
	{
		EFDirectIndex dir = (EFDirectIndex) efIndex.getDirectIndex();
		for (int termid = 0; termid < efIndex.getCollectionStatistics().getNumberOfUniqueTerms(); ++termid) {
			LexiconEntry le = efIndex.getLexicon().getLexiconEntry(termid).getValue();
			IterablePosting ip = efIndex.getInvertedIndex().getPostings(le);
			while (ip.next() != IterablePosting.EOL) {
				IterablePosting dp = dir.getPostings(ip.getId());
				dp.next(termid);
				assertEquals(dp.getId(), termid);
				assertEquals(dp.getFrequency(), ip.getFrequency());
				dp.close();
			}
			ip.close();
		}
		dir.close();
	}

	@Test
	public void testDir2InvContents() throws IOException
	{
		EFInvertedIndex inv = (EFInvertedIndex) efIndex.getInvertedIndex();
		EFDirectIndex dir = (EFDirectIndex) efIndex.getDirectIndex();
		for (int docid = 0; docid < efIndex.getCollectionStatistics().getNumberOfDocuments(); ++docid) {
			IterablePosting dp = dir.getPostings(docid);
			while (dp.next() != IterablePosting.EOL) {
				IterablePosting ip = inv.getPostings(efIndex.getLexicon().getLexiconEntry(dp.getId()).getValue());
				ip.next(docid);
				assertEquals(ip.getId(), docid);
				assertEquals(ip.getFrequency(), dp.getFrequency());
				ip.close();
			}
			dp.close();
		}
		dir.close();
		inv.close();
	}

	/*
	private void printinv() throws IOException
	{
		for (int termid = 0; termid < efIndex.getCollectionStatistics().getNumberOfUniqueTerms(); ++termid) {
			LexiconEntry le = efIndex.getLexicon().getLexiconEntry(termid).getValue();
			IterablePosting ip = efIndex.getInvertedIndex().getPostings(le);
			System.out.print(termid + " : ");
			while (ip.next() != IterablePosting.EOL) {
				System.out.print(ip.getId() + " ");
			}
			System.out.println();
			ip.close();
		}
		
	}
	
	private void printdir() throws IOException
	{
		for (int docid = 0; docid < efIndex.getCollectionStatistics().getNumberOfDocuments(); ++docid) {
			IterablePosting ip = ((EFDirectIndex)(efIndex.getDirectIndex())).getPostings(docid);
			System.out.print(docid + " : ");
			while (ip.next() != IterablePosting.EOL) {
				System.out.print(ip.getId() + " ");
			}
			System.out.println();
			ip.close();
		}
		
	}
	*/
}
