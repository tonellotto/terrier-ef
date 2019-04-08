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
package it.cnr.isti.hpclab.ef.structures;

import it.cnr.isti.hpclab.ef.EliasFano;
import it.unimi.dsi.fastutil.longs.LongBigList;
import it.unimi.dsi.util.ByteBufferLongBigList;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel.MapMode;
import java.io.FileInputStream;

import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.Pointer;
import org.terrier.structures.PostingIndex;
import org.terrier.structures.postings.IterablePosting;

/**
 * Class to access an Elias-Fano encoded inverted index in Terrier.
 */
public class EFInvertedIndex implements PostingIndex<BitIndexPointer>
{
	protected final IndexOnDisk index;
	protected final DocumentIndex doi;
	
	protected final LongBigList docidsList;
	protected final LongBigList freqsList;
	protected final LongBigList posList;
	
	/**
	 * Constructor
	 * @param index the index containing the inverted index
	 * @param structureName
	 * @throws IOException
	 */
	public EFInvertedIndex(final IndexOnDisk index) throws IOException 
	{
		this(index, index.getDocumentIndex());
	}

	@SuppressWarnings("resource")
	public EFInvertedIndex(final IndexOnDisk index, final DocumentIndex _doi) throws IOException
	{
		this.index = index;
		this.doi = _doi;

		String byteOrderString = index.getIndexProperty(EliasFano.BYTEORDER, "");
		ByteOrder byteOrder;
		if ("LITTLE_ENDIAN".equals(byteOrderString))
			byteOrder = ByteOrder.LITTLE_ENDIAN;
		else if ("BIG_ENDIAN".equals(byteOrderString))
			byteOrder = ByteOrder.BIG_ENDIAN;
		else
			throw new IllegalStateException();

		docidsList  = ByteBufferLongBigList.map( new FileInputStream( index.getPath() + File.separator + index.getPrefix() + EliasFano.DOCID_EXTENSION ).getChannel(), byteOrder, MapMode.READ_ONLY );
		freqsList   = ByteBufferLongBigList.map( new FileInputStream( index.getPath() + File.separator + index.getPrefix() + EliasFano.FREQ_EXTENSION  ).getChannel(), byteOrder, MapMode.READ_ONLY );
		
		if (hasPositions())
			posList = ByteBufferLongBigList.map( new FileInputStream( index.getPath() + File.separator + index.getPrefix() + EliasFano.POS_EXTENSION   ).getChannel(), byteOrder, MapMode.READ_ONLY );
		else
			posList = null;
	}
	
	/** {@inheritDoc} */
	@Override
	public IterablePosting getPostings(final Pointer pointer) throws IOException 
	{
		int df 			 = ((EFLexiconEntry)pointer).getDocumentFrequency();
		long docidOffset = ((EFLexiconEntry)pointer).getDocidOffset();
		long freqOffset  = ((EFLexiconEntry)pointer).getFreqOffset();
		int F 			 = ((EFLexiconEntry)pointer).getFrequency();
		
		int N 			 = index.getCollectionStatistics().getNumberOfDocuments();
		int log2Quantum  = index.getIntIndexProperty(EliasFano.LOG2QUANTUM, 0);
		
		// Sanity check
		if (log2Quantum == 0)
			throw new RuntimeException();
		
		IterablePosting rtr = null;
		if (hasPositions()) {
			long posOffset  = ((EFBlockLexiconEntry)pointer).getPosOffset();
			rtr = new EFBlockIterablePosting(docidsList, freqsList, posList, doi, df, N, F, log2Quantum, docidOffset, freqOffset, posOffset);
		} else {
			rtr = new EFBasicIterablePosting(docidsList, freqsList, doi, df, N, F, log2Quantum, docidOffset, freqOffset);
		}
		return rtr;
	}
	
	/** 
	 * Nothing to close.
	 */
	@Override
	public void close()
	{
	}

	/**
	 * Return true if the index contains positional information, false otherwise.
	 * @return true if the index contains positional information, false otherwise.
	 */
	public boolean hasPositions()
	{
		 return "true".equals(index.getIndexProperty(EliasFano.HAS_POSITIONS, "false"));
	}
}