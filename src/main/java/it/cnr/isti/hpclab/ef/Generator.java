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

import it.cnr.isti.hpclab.ef.structures.EFDocumentIndex;
import it.cnr.isti.hpclab.ef.structures.EFLexiconEntry;

import it.cnr.isti.hpclab.ef.util.IndexUtil;
import it.cnr.isti.hpclab.ef.util.LongWordBitWriter;
import it.cnr.isti.hpclab.ef.util.SequenceEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.terrier.structures.BasicLexiconEntry;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.FSOMapFileLexiconOutputStream;
import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.LexiconOutputStream;
import org.terrier.structures.collections.FSOrderedMapFile;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.structures.seralization.FixedSizeTextFactory;

import org.terrier.utility.TerrierTimer;

/**
 * This program converts an existing simple Terrier 5 index (no positions) into a Elias-Fano compressed Terrier 5 index.
 */
public class Generator 
{
	public static int LOG2QUANTUM = Integer.parseInt(System.getProperty(EliasFano.LOG2QUANTUM, "8"));
	private static ByteOrder BYTEORDER = ByteOrder.nativeOrder();
	private static int DEFAULT_CACHE_SIZE = 64 * 1024 * 1024;
	
	public static void link(String from, String to) throws IOException
	{
		Runtime.getRuntime().exec("ln -s " + from + " " + to);  
	}
	
	public static void main(final String args[]) throws IOException 
	{
		Index.setIndexLoadingProfileAsRetrieval(false);
		
		if (args.length != 2) {
			System.err.println("Usage: java it.cnr.isti.hpclab.ef.Generator <index.path> <src.index.prefix>");
			System.exit(-1);
		}

		String path = args[0];
		String srcPrefix = args[1];
		String dstPrefix = srcPrefix + EliasFano.USUAL_EXTENSION;

		IndexOnDisk srcIndex;

		srcIndex = Index.createIndex(path, srcPrefix);
		if (Index.getLastIndexLoadError() != null) {
			System.err.println(Index.getLastIndexLoadError());
			System.exit(-2);
		}
				
		createLexiconDocidsFreqs(path, dstPrefix, srcIndex);
		createDocumentIndex(path, dstPrefix, srcIndex);
		// createMetaIndex(path, dstPrefix, srcIndex);
		createProperties(path, dstPrefix, srcIndex);
		
		IndexOnDisk dstIndex;
		dstIndex = Index.createIndex(path, dstPrefix);
		if (Index.getLastIndexLoadError() != null) {
			System.err.println(Index.getLastIndexLoadError());
			System.exit(-2);
		}
				
		randomSanityCheck(srcIndex, dstIndex);
	}

	public static void randomSanityCheck(IndexOnDisk srcIndex, IndexOnDisk dstIndex) throws IOException 
	{
		Random rnd = new Random(System.currentTimeMillis());
		int numSamples = 50 + 1 + rnd.nextInt(50);
		System.err.println("Randomly checking " + numSamples + " terms and posting list for sanity check");
		
		if (srcIndex.getCollectionStatistics().getNumberOfUniqueTerms() != dstIndex.getCollectionStatistics().getNumberOfUniqueTerms()) {
			System.err.println("Original index has " + srcIndex.getCollectionStatistics().getNumberOfUniqueTerms() + " unique terms");
			System.err.println("Elias-Fano index has " + dstIndex.getCollectionStatistics().getNumberOfUniqueTerms() + " unique terms");
			System.exit(-1);
		}

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

			IterablePosting srcPosting = srcIndex.getInvertedIndex().getPostings(ble);
			IterablePosting dstPosting = dstIndex.getInvertedIndex().getPostings(efle);
						
			while (srcPosting.next() != IterablePosting.END_OF_LIST && dstPosting.next() != IterablePosting.END_OF_LIST) {
				if ((srcPosting.getId() != dstPosting.getId()) || (srcPosting.getFrequency() != dstPosting.getFrequency())) {
					System.err.println("Something went wrong in random sanity check...");
					System.exit(-1);
				}
			}
		}
	}

	private static void createProperties(final String path, final String dstPrefix, final IndexOnDisk srcIndex) throws IOException 
	{		
		String filename = path + File.separator + dstPrefix + ".properties";
		int num_docs = srcIndex.getCollectionStatistics().getNumberOfDocuments();
		int num_terms = srcIndex.getCollectionStatistics().getNumberOfUniqueTerms();
		long num_pointers = srcIndex.getCollectionStatistics().getNumberOfPointers();
		long num_tokens = srcIndex.getCollectionStatistics().getNumberOfTokens();
		
		IndexUtil.writeEFIndexProperties(filename, num_docs, num_terms, num_pointers, num_tokens, LOG2QUANTUM);
  	}

	private static void createDocumentIndex(final String path, final String dstPrefix, final IndexOnDisk srcIndex) throws IOException 
	{
		EFDocumentIndex.write((org.terrier.structures.DocumentIndex) srcIndex.getDocumentIndex(), path + File.separator + dstPrefix + ".sizes");
	}

	@SuppressWarnings("resource")
	private static void createLexiconDocidsFreqs(final String path, final String dstPrefix, final IndexOnDisk srcIndex) throws IOException 
	{
		// The new lexicon writer (please note it is an output stream)
		LexiconOutputStream<String> los = new FSOMapFileLexiconOutputStream(path + File.separator + dstPrefix + ".lexicon" + FSOrderedMapFile.USUAL_EXTENSION, new FixedSizeTextFactory(IndexUtil.DEFAULT_MAX_TERM_LENGTH));
				
		int numberOfDocuments = srcIndex.getCollectionStatistics().getNumberOfDocuments();
		// The sequence encoder to generate posting lists (docids)
		SequenceEncoder docidsAccumulator = new SequenceEncoder( DEFAULT_CACHE_SIZE, LOG2QUANTUM );
		// The sequence encoder to generate posting lists (freqs)
		SequenceEncoder freqsAccumulator = new SequenceEncoder( DEFAULT_CACHE_SIZE, LOG2QUANTUM );

		// The new docids inverted file
		LongWordBitWriter docids = new LongWordBitWriter( new FileOutputStream(path + File.separator + dstPrefix + EliasFano.DOCID_EXTENSION).getChannel(), BYTEORDER );
		// The new freqs inverted file
		LongWordBitWriter freqs = new LongWordBitWriter( new FileOutputStream(path + File.separator + dstPrefix + EliasFano.FREQ_EXTENSION).getChannel(), BYTEORDER );

		TerrierTimer tt = new TerrierTimer("Creating EliasFano Lexicon, Docids and Frequencies", srcIndex.getCollectionStatistics().getNumberOfUniqueTerms());tt.start();    
		Iterator<Entry<String, LexiconEntry>> lexiconIterator = srcIndex.getLexicon().iterator();
				
		long docidsOffset = 0;
		long freqsOffset = 0;
		
		int cnt = 0;
		while (lexiconIterator.hasNext()) {
			// We get the next lexicon entry from the source index (assuming them ordered by termid)
			Map.Entry<String, LexiconEntry> leIn = lexiconIterator.next();
			LexiconEntry le = leIn.getValue();
				    	    	
			// We create the new lexicon entry with skip offset data included
			EFLexiconEntry leOut = new EFLexiconEntry( le.getTermId(), le.getDocumentFrequency(), le.getFrequency(), docidsOffset, freqsOffset);
				       	
			// We write the new lexicon entry to the new lexicon
			los.writeNextEntry(leIn.getKey(), leOut);
					 
			IterablePosting p = srcIndex.getInvertedIndex().getPostings((BitIndexPointer)le);
					 			
			docidsAccumulator.init( le.getDocumentFrequency(), numberOfDocuments, false, true, LOG2QUANTUM );
			freqsAccumulator.init(  le.getDocumentFrequency(), le.getFrequency(), true, false, LOG2QUANTUM );

			long lastDocid = 0;
			while (p.next() != IterablePosting.END_OF_LIST) {
				docidsAccumulator.add( p.getId() - lastDocid );
				lastDocid = p.getId();
				freqsAccumulator.add(p.getFrequency());
			}
						
			docidsOffset += docidsAccumulator.dump(docids);		
			freqsOffset  += freqsAccumulator.dump(freqs);
			
			tt.increment();
			cnt++;
		}
		tt.finished();
		System.err.println("Total " + srcIndex.getCollectionStatistics().getNumberOfUniqueTerms() + ", processed " + cnt);
				
		docidsAccumulator.close();
		docids.close();
		freqsAccumulator.close();
		freqs.close();
		los.close();
	}
}