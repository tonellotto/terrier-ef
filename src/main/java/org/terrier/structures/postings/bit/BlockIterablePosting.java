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

/*
 * The original source code is org.terrier.structures.postings.bit.BlockIterablePosting class
 * 
 * http://terrier.org/docs/v5.0/javadoc/org/terrier/structures/postings/bit/BlockIterablePosting.html
 * 
 * being part of
 *  		 
 * Terrier - Terabyte Retriever
 *
 * Copyright (C) 2004-2018 the University of Glasgow. 
 */

/**
 * Ugly workaround using class masking (don't try this at home) waiting TR-519 bug report to be fixed.
 */
package org.terrier.structures.postings.bit;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableUtils;

import org.terrier.compression.bit.BitIn;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.BlockPostingImpl;
import org.terrier.structures.postings.WritablePosting;
import org.terrier.utility.ArrayUtils;
/** 
 * A writable block iterable posting list
 */
public class BlockIterablePosting extends BasicIterablePosting implements BlockPosting
{
	private static final long serialVersionUID = 1L;
	private int[] positions;
	/**
	 * Constructs an instance of the BlockIterablePosting.
	 */
	public BlockIterablePosting(){super();}
	/** 
	 * Constructs an instance of the BlockIterablePosting.
	 * @param _bitFileReader
	 * @param _numEntries
	 * @param doi
	 * @throws IOException
	 */
	public BlockIterablePosting(BitIn _bitFileReader, int _numEntries, DocumentIndex doi) throws IOException {
		super(_bitFileReader, _numEntries, doi);
	}
	
	/** {@inheritDoc} */
	public int[] getPositions() {
		return positions;
	}

	@Override
	public int next() throws IOException 
	{
	    if (numEntries == 0) {
			id = END_OF_LIST;
		} else {
			id += bitFileReader.readGamma();
			tf = bitFileReader.readUnary();
			numEntries--;
			//TODO: this has a memory allocation for every posting in the posting list. can we reuse an array?
			positions = new int[bitFileReader.readUnary() -1];
			if (positions.length == 0)
				return id;
			positions[0] = bitFileReader.readGamma() -1;
			for(int i=1;i<positions.length;i++)
				positions[i] = positions[i-1] + bitFileReader.readGamma();
		}
		return id;
	}
	
	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		final int blockCount = WritableUtils.readVInt(in);
		positions = new int[blockCount]; 
		for(int i=0;i<blockCount;i++)
			positions[i] = WritableUtils.readVInt(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		WritableUtils.writeVInt(out, positions.length);
		for(int pos : positions)
			WritableUtils.writeVInt(out, pos);
	}

	@Override
	public WritablePosting asWritablePosting() {
		int[] newPositions = new int[positions.length];
		System.arraycopy(positions, 0, newPositions, 0, positions.length);
		return new BlockPostingImpl(getId(), getFrequency(), newPositions);
	}

	@Override
	public String toString()
	{
		return "(" + id + "," + tf + ",B[" + ArrayUtils.join(positions, ",") + "])";
	}
}
