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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terrier.querying.IndexRef;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.Index;

import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.utility.Files;

import it.cnr.isti.hpclab.ef.structures.EFBlockLexiconEntry;
import it.cnr.isti.hpclab.ef.util.SynchronizedProgressBar;

/**
 * This is a Elias-Fano compressor focusing on lexicon and posting lists only. It compresses only a range of input termids.
 * All lexicon entries have offsets aligned to this portion of the whole index only, and the docis/freqs/pos files are closed at the end, so such files are byte-aligned.
 * This must be taken into account when merging.
 */
public class BlockCompressor implements Compressor
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(BlockCompressor.class);
    protected static final int DEFAULT_CACHE_SIZE = 64 * 1024 * 1024;

    protected int LOG2QUANTUM;
    
    protected final IndexRef dstRef;
    
    protected final Index srcIndex;
    protected final int numDocs;

    /**
     * Constructor.
     * 
     * @param srcIndex source index
     * @param dstRef destination index reference
     */
    public BlockCompressor(final Index srcIndex, final IndexRef dstRef)
    {
        this(srcIndex, dstRef, Integer.parseInt(System.getProperty(EliasFano.LOG2QUANTUM, "8")));
    }
    
    /**
     * Constructor.
     * 
     * @param srcIndex source index
     * @param dstRef destination index reference
     * @param log2quantum log2 of quantum
     */
    public BlockCompressor(final Index srcIndex, final IndexRef dstRef, final int log2quantum)
    {
        this.dstRef = dstRef;
        
        if (Files.exists(dstRef.toString())) {
            LOGGER.error("Cannot compress index while an index already exists at " + dstRef);
            this.srcIndex = null;
            this.numDocs = 0;
            return;
        }        
        this.srcIndex = srcIndex;        
        this.numDocs = srcIndex.getCollectionStatistics().getNumberOfDocuments();
        
        this.LOG2QUANTUM = log2quantum;
    }
    
    @Override
    public void compress(final TermPartition terms) throws IOException
    {      
		// opening src index lexicon iterator and moving to the begin termid
		Iterator<Entry<String, LexiconEntry>> lexIter = srcIndex.getLexicon().iterator();
		Entry<String, LexiconEntry> lee = advanceLexiconIteratorTo(terms.begin(), lexIter);

		// writers
		final String dstIndexPath = FilenameUtils.getFullPath(dstRef.toString()) + File.separator + terms.prefix();
		EliasFanoBlockWriters befWriters = new EliasFanoBlockWriters(dstIndexPath);
		EliasFanoBlockEncoders befEncoders = new EliasFanoBlockEncoders(LOG2QUANTUM);
		
        LexiconEntry le = null;
        IterablePosting p = null;
        
        while (!stop(lee, terms.end())) {
            le = lee.getValue();
            befWriters.writeLexiconEntry(lee.getKey(), new EFBlockLexiconEntry(le.getTermId(), le.getDocumentFrequency(), le.getFrequency(), le.getMaxFrequencyInDocuments(), befWriters.docidBitOffset, befWriters.freqBitOffset, befWriters.posBitOffset));
            
            befEncoders.init(le.getDocumentFrequency(), numDocs, le.getFrequency());
            
            p = srcIndex.getInvertedIndex().getPostings((BitIndexPointer)le);
            befEncoders.add(p);
            p.close();
            
            p = srcIndex.getInvertedIndex().getPostings((BitIndexPointer)le);
            befEncoders.addPositions(p);
            p.close();

            befEncoders.dump(befWriters);            
        
            lee = lexIter.hasNext() ? lexIter.next() : null;
            SynchronizedProgressBar.getInstance().step();
        }

        befEncoders.close();
        befWriters.close();
    }    
}