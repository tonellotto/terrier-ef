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
package it.cnr.isti.hpclab.ef.structures;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel.MapMode;

import org.terrier.structures.DocumentIndex;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.Pointer;
import org.terrier.structures.PostingIndex;
import org.terrier.structures.collections.FSArrayFile;
import org.terrier.structures.postings.IterablePosting;

import it.cnr.isti.hpclab.ef.DirectIndexWriter;
import it.cnr.isti.hpclab.ef.EliasFano;
import it.unimi.dsi.fastutil.longs.LongBigList;
import it.unimi.dsi.util.ByteBufferLongBigList;

/**
 * Class to access an Elias-Fano encoded direct index in Terrier.
 * It is identical to an Elias-Fano encoded inverted index, indexed by docids.
 */

public class EFDirectIndex implements PostingIndex<Pointer>
{
	protected final IndexOnDisk index;
	protected final DocumentIndex doi;
	
	protected final RandomAccessFile raf;
	protected final LongBigList termidsList;
	protected final LongBigList freqsList;
	// protected final LongBigList posList;
	
	// upper bounds to use
	protected final int upperBoundTermids;
	// protected final DocumentIndex doi;
	
	/**
	 * Constructor
	 * @param index the index containing the direct index
	 * @param structureName
	 * @throws IOException
	 */
	@SuppressWarnings("resource")
	public EFDirectIndex(final IndexOnDisk index) throws IOException 
	{
		this.index = index;
		this.doi   = index.getDocumentIndex();

		String byteOrderString = index.getIndexProperty(EliasFano.BYTEORDER, "");
		ByteOrder byteOrder;
		if ("LITTLE_ENDIAN".equals(byteOrderString))
			byteOrder = ByteOrder.LITTLE_ENDIAN;
		else if ("BIG_ENDIAN".equals(byteOrderString))
			byteOrder = ByteOrder.BIG_ENDIAN;
		else
			throw new IllegalStateException();

		raf         = new RandomAccessFile(index.getPath() + File.separator + index.getPrefix() + ".direct" + FSArrayFile.USUAL_EXTENSION, "r");
		termidsList = ByteBufferLongBigList.map( new FileInputStream( index.getPath() + File.separator + index.getPrefix() + ".direct" + EliasFano.DOCID_EXTENSION ).getChannel(), byteOrder, MapMode.READ_ONLY );
		freqsList   = ByteBufferLongBigList.map( new FileInputStream( index.getPath() + File.separator + index.getPrefix() + ".direct" + EliasFano.FREQ_EXTENSION  ).getChannel(), byteOrder, MapMode.READ_ONLY );
		
		this.upperBoundTermids   = index.getCollectionStatistics().getNumberOfUniqueTerms();

		/*
		if (hasPositions())
			posList = ByteBufferLongBigList.map( new FileInputStream( index.getPath() + File.separator + index.getPrefix() + ".direct" + EliasFano.POS_EXTENSION   ).getChannel(), byteOrder, MapMode.READ_ONLY );
		else
			posList = null;
		*/
	}
	
	public IterablePosting getPostings(final int docid) throws IOException 
	{
		raf.seek((long)docid * DirectIndexWriter.ENTRY_SIZE);

		long termidOffset = raf.readLong();
		long freqOffset   = raf.readLong();
		int pl_size 	  = raf.readInt();
		
		IterablePosting rtr = null;
		//if (hasPositions()) {
		//	long posOffset  = ((EFBlockLexiconEntry)pointer).getPosOffset();
		//	rtr = new EFBlockIterablePosting(docidsList, freqsList, posList, doi, df, N, F, log2Quantum, docidOffset, freqOffset, posOffset);
		//} else {
		
			rtr = new EFBasicIterablePosting(termidsList, freqsList, doi, 
										     pl_size, upperBoundTermids,  doi.getDocumentLength(docid), DirectIndexWriter.LOG2QUANTUM, 
										     termidOffset, freqOffset);
		//}
		return rtr;
	}
	
	/**
	 * Return true if the index contains positional information, false otherwise.
	 * @return true if the index contains positional information, false otherwise.
	 */
	public boolean hasPositions()
	{
		// return "true".equals(index.getIndexProperty(EliasFano.HAS_POSITIONS, "false"));
		return false;
	}

	@Override
	public void close() throws IOException 
	{
		raf.close();
	}

	@Override
	public IterablePosting getPostings(Pointer lEntry) throws IOException 
	{
		return getPostings(((EFDocumentIndexEntry)lEntry).docid);
	}	
}
