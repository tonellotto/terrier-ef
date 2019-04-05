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

import it.cnr.isti.hpclab.ef.util.DocidReader;
import it.cnr.isti.hpclab.ef.util.FreqReader;
import it.cnr.isti.hpclab.ef.util.LongWordBitReader;
import it.cnr.isti.hpclab.ef.util.Utils;
import it.unimi.dsi.fastutil.longs.LongBigList;

import java.io.IOException;

import org.terrier.structures.DocumentIndex;
import org.terrier.structures.postings.BasicPostingImpl;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.structures.postings.WritablePosting;

/**
 * Elias-Fano implementation of an iterable posting, i.e., a posting cursor over a posting list.
 */
public class EFBasicIterablePosting implements IterablePosting
{
	private DocumentIndex doi;
	
	protected DocidReader docidReader = null;
	protected FreqReader freqReader = null;
	protected long currentDocument;
	protected long currentFrequency;
	protected long N;
	
	/** 
	 * Create an empty EFBasicIterablePosting.
	 */
	public EFBasicIterablePosting()
	{	
	}

	/**
	 * Create a EFBasicIterablePosting object.
	 * 
	 * @param _docidList the Elias-Fano compressed list view to access to read docids
	 * @param _freqList the Elias-Fano compressed list view to access to read frequencies
	 * @param doi the document index to use to read document lengths
	 * @param numEntries number of postings in the posting list
	 * @param upperBoundDocid upper bound on the docids
	 * @param upperBoundFreq upper bound on the frequency
	 * @param log2Quantum the quantum used to encode forward (skip) pointers
	 * @param docidsPosition the initial bit offset in the docids file of this posting list
	 * @param freqsPosition the initial bit offset in the freq file of this posting list
	 */
	public EFBasicIterablePosting(final LongBigList docidList, final LongBigList freqList, final DocumentIndex doi,
								  final int numEntries, final int upperBoundDocid, final int upperBoundFreq, final int log2Quantum,
								  final long docidsPosition, final long freqsPosition)
	{
		this.doi = doi;
		this.N = upperBoundDocid;
		
		
		// the number of lower bits for the EF encoding of a list of given length, upper bound and strictness.
		int l = Utils.lowerBits( numEntries + 1, upperBoundDocid, false );
		// the size in bits of forward or skip pointers to the EF encoding of a list of given length, upper bound and strictness.
		int pointerSize = Utils.pointerSize( numEntries + 1, upperBoundDocid, false, true );
		// the number of skip pointers to the EF encoding of a list of given length, upper bound and strictness.
		long numberOfPointers = Utils.numberOfPointers( numEntries + 1, upperBoundDocid, log2Quantum, false, true );

		// Reader of elements of size pointerSize
		LongWordBitReader skipPointers = new LongWordBitReader( docidList, pointerSize );
		// Reader of elements of size l
		LongWordBitReader lowerBits = new LongWordBitReader( docidList, l );

		long skipPointersStart = docidsPosition;
		// Where to start reading the lower bits array
		long lowerBitsStart = skipPointersStart + pointerSize * numberOfPointers;
		lowerBits.position( lowerBitsStart ); 						
				
		this.docidReader = new DocidReader( docidList, lowerBits, lowerBitsStart, l, skipPointers, skipPointersStart, numberOfPointers, pointerSize, numEntries, log2Quantum );
		currentDocument = -2;
		
		this.freqReader = new FreqReader( freqList, freqsPosition, numEntries, upperBoundFreq, log2Quantum );
		currentFrequency = 0;
	}
	
	/** {@inheritDoc} */
	@Override
	public int getId() 
	{
		return (int) currentDocument;
	}

	/** {@inheritDoc} */
	@Override
	public int getFrequency() 
	{
		return (int) currentFrequency;
	}

	/** {@inheritDoc} */
	@Override
	public int getDocumentLength() 
	{
		try {
			return doi.getDocumentLength((int) currentDocument);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void setId(int id) 
	{
		throw new UnsupportedOperationException();
		
	}

	/** {@inheritDoc} */
	@Override
	public WritablePosting asWritablePosting() 
	{
		return new BasicPostingImpl((int) currentDocument, (int) currentFrequency);
	}

	/** {@inheritDoc} */
	@Override
	public void close() throws IOException 
	{
	}

	/** {@inheritDoc} */
	@Override
	public int next() throws IOException 
	{
		if ( currentDocument == IterablePosting.END_OF_LIST ) 
			return IterablePosting.END_OF_LIST;

		if ( ( currentDocument = docidReader.getNextPrefixSum() ) >= N ) {
			currentDocument = IterablePosting.END_OF_LIST;
		} else {
			currentFrequency = freqReader.getLong( docidReader.currentIndex - 1 );
		}
		
		return (int) currentDocument;	
	}

	/** {@inheritDoc} */
	@Override
	public int next(int targetId) throws IOException 
	{
		if ( targetId >= N ) 
			return (int) (currentDocument = IterablePosting.END_OF_LIST);

		if ( currentDocument >= targetId ) 
			return (int) currentDocument;

		if ( ( currentDocument = docidReader.skipTo( targetId ) ) >= N ) {
			currentDocument = IterablePosting.END_OF_LIST;
		}  else {
			currentFrequency = freqReader.getLong( docidReader.currentIndex - 1 );
		}
		
		return (int) currentDocument;
	}

	/** {@inheritDoc} */
	@Override
	public boolean endOfPostings() 
	{
		return (currentDocument == IterablePosting.END_OF_LIST);
	}
	
	/** {@inheritDoc} */
	@Override
	public String toString()
	{
		return "(" + currentDocument + "," + currentFrequency + ")";
	}
	
	/**
	 * Returns the position of the current docid w.r.t. the beginning of the list (indexed at 0).
	 *  
	 * @return The position of the current docid w.r.t. the beginning of the list.
	 */
	public long getCurrentDocidPosition()
	{
		return docidReader.getCurrentPos() - 1;
	}
}
