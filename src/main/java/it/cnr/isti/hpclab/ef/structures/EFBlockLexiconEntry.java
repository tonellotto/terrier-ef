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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.terrier.structures.LexiconEntry;

/** 
 * Contains all the information about one entry with positions in the Lexicon.
 * Based on the BlockLexiconEntry class in Terrier. 
 * Include offsets for positions file compressed with Elias-Fano.  
 */

public class EFBlockLexiconEntry extends EFLexiconEntry
{
	private static final long serialVersionUID = 1L;
	
	/** the offsets we need */
	public long posOffset;

	/** upper bound used in Elias-Fano */
	private long sumsMaxPos = 0l;
	
	/** 
	 * Factory for creating EFBlockLexiconEntry objects
	 */
	public static class Factory extends EFLexiconEntry.Factory
	{
		/** {@inheritDoc} */
		@Override
		public int getSize() 
		{
			return super.getSize() + 2 * Long.BYTES;
		}
		
		/** {@inheritDoc} */
		@Override
		public LexiconEntry newInstance() 
		{
			return new EFBlockLexiconEntry();
		}
	}
	
	/** 
	 * Create an empty EFBlockLexiconEntry.
	 */
	public EFBlockLexiconEntry() 
	{
	}

	/** 
	 * Create a block lexicon entry with the following information:
	 * 
	 * @param tid the term id
	 * @param n_t the number of documents the term occurs in (document frequency)
	 * @param TF the total count of term t in the collection
	 * @param docidOffset the bit offset of the posting list in the docid file
	 * @param freqOffset the bit offset of the posting list in the freq file
	 * @param posOffset the bit offset of the posting list in the positions file
	 * @param sumsMaxPos upper bound used in Elias-Fano
	 */
	public EFBlockLexiconEntry(int tid, int n_t, int TF, long docidOffset, long freqOffset, long posOffset, long sumsMaxPos) 
	{
		super(tid, n_t, TF, Integer.MAX_VALUE, docidOffset, freqOffset);
		this.posOffset = posOffset;
		this.sumsMaxPos = sumsMaxPos;
	}
	
	/** 
	 * Create a lexicon entry with the following information:
	 * 
	 * @param tid the term id
	 * @param n_t the number of documents the term occurs in (document frequency)
	 * @param TF the total count of term t in the collection
	 * @param maxtf the largest in-document term frequency in the posting list
	 * @param docidOffset the bit offset of the posting list in the docid file
	 * @param freqOffset the bit offset of the posting list in the freq file
	 * @param posOffset the bit offset of the posting list in the positions file
	 * @param sumsMaxPos upper bound used in Elias-Fano
	 */
	public EFBlockLexiconEntry(int tid, int n_t, int TF, int maxtf, long docidOffset, long freqOffset, long posOffset, long sumsMaxPos) 
	{
		super(tid, n_t, TF, maxtf, docidOffset, freqOffset);
		this.posOffset = posOffset;
		this.sumsMaxPos = sumsMaxPos;
	}

	/** {@inheritDoc} */
	@Override
	public void readFields(DataInput in) throws IOException 
	{
		super.readFields(in);
		this.posOffset = in.readLong();
		this.sumsMaxPos = in.readLong();
	}
	
	/** {@inheritDoc} */
	@Override
	public void write(DataOutput out) throws IOException 
	{
		super.write(out);
		out.writeLong(this.posOffset);
		out.writeLong(this.sumsMaxPos);
	}

	/** {@inheritDoc} */
	@Override
	public String toString()
	{
		return super.toString() + " [sumsMaxPos = " +  this.sumsMaxPos + " pos @ " + this.posOffset + "]";
	}
	
	/**
	 * Return the bit offset of the posting list in the positions file
	 * @return the bit offset of the posting list in the positions file
	 */
	public long getPosOffset() 
	{
		return this.posOffset;
	}

	/**
	 * Return upper bound used in Elias-Fano
	 * @return upper bound used in Elias-Fano
	 */
	public long getSumsMaxPos()
	{
		return this.sumsMaxPos;
	}
}