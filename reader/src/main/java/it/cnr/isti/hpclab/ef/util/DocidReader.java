/*
 * The original source code is it.unimi.di.big.mg4j.index.QuasiSuccinctIndexReader class
 * 
 * http://mg4j.di.unimi.it/docs-big/it/unimi/di/big/mg4j/index/QuasiSuccinctIndexReader.html
 * 
 * being part of
 *  		 
 * MG4J: Managing Gigabytes for Java (big)
 *
 * Copyright (C) 2012 Sebastiano Vigna 
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

package it.cnr.isti.hpclab.ef.util;

import static it.unimi.dsi.bits.Fast.MSBS_STEP_8;
import static it.unimi.dsi.bits.Fast.ONES_STEP_4;
import static it.unimi.dsi.bits.Fast.ONES_STEP_8;

import it.unimi.dsi.bits.Fast;
import it.unimi.dsi.fastutil.longs.LongBigList;

public class DocidReader 
{
	/** The underlying list. */
	protected final LongBigList list;
	/** The longword bit reader for pointers. */
	protected final LongWordBitReader skipPointers;
	/** The starting position of the pointers. */
	protected final long skipPointersStart;
	/** The starting position of the upper bits. */
	protected final long upperBitsStart;
	/** The logarithm of the quantum, cached from the index. */
	protected final int log2Quantum;
	/** The quantum, cached from the index. */
	protected final int quantum;
	/** The size of a pointer. */
	protected final int pointerSize;
	/** The number of pointers. */
	protected final long numberOfPointers;
	/** The frequency of the term (i.e., the number of elements of the current list). */
	protected final long frequency;
	/** The 64-bit window. */
	protected long window;
	/** The current word position in the list of upper bits. */
	protected long curr;
	/** The index of the current prefix sum. */
	public long currentIndex;
		
	private final static int SKIPPING_THRESHOLD = 8;
	/** The number of lower bits. */
	private final int l;
	/** The longword bit reader for the lower bits. */
	private final LongWordBitReader lowerBits;
	/** The starting position of the power bits. */
	private final long lowerBitsStart;
	/** The last value returned by {@link #getNextUpperBits()}. */ 
	private long lastUpperBits;
		
	public DocidReader( final LongBigList list, final LongWordBitReader lowerBits, final long lowerBitsStart, final int l, final LongWordBitReader skipPointers, final long skipPointersStart, final long numberOfPointers, final int pointerSize, final long frequency, final int log2Quantum ) 
	{
		// this( list, lowerBitsStart + l * ( frequency + 1L ), skipPointers, skipPointersStart, numberOfPointers, pointerSize, frequency, log2Quantum );
		
		this.list = list;
		this.upperBitsStart = lowerBitsStart + l * ( frequency + 1L );
		this.skipPointers = skipPointers;
		this.skipPointersStart = skipPointersStart;
				
		this.pointerSize = pointerSize;
		this.numberOfPointers = numberOfPointers;
		this.log2Quantum = log2Quantum;
		this.quantum = 1 << log2Quantum;
		this.frequency = frequency;
		
		this.lowerBits = lowerBits;
		this.lowerBitsStart = lowerBitsStart;
		this.l = l;
		position( upperBitsStart );
	}

	private void position( final long position ) 
	{
		window = list.getLong( curr = position / Long.SIZE ) & -1L << (int)( position );
	}

	private long getNextUpperBits() 
	{
		while ( window == 0 ) 
			window = list.getLong( ++curr );
		lastUpperBits = curr * Long.SIZE + Long.numberOfTrailingZeros( window ) - currentIndex++ - upperBitsStart;
		window &= window - 1;
		return lastUpperBits;
	}

	public long getNextPrefixSum() 
	{
		return getNextUpperBits() << l | lowerBits.extract();
	}

	public long skipTo( final long lowerBound ) 
	{
		final long zeroesToSkip = lowerBound >>> l;

		if ( zeroesToSkip - lastUpperBits < SKIPPING_THRESHOLD ) {
			long prefixSum;
			while( ( prefixSum = getNextPrefixSum() ) < lowerBound )
				;
			return prefixSum;
		}
			
		if ( zeroesToSkip - lastUpperBits > quantum ) {
			final long block = zeroesToSkip >>> log2Quantum;
			assert block > 0;
			assert block <= numberOfPointers;
			final long blockZeroes = block << log2Quantum;
			final long skip = skipPointers.extract( skipPointersStart + ( block - 1 ) * pointerSize );
			assert skip != 0;
			position( upperBitsStart + skip );
			currentIndex = skip - blockZeroes;
		}

		long delta = zeroesToSkip - curr * Long.SIZE + currentIndex + upperBitsStart;			
		assert delta >= 0 : delta;

		for( int bitCount; ( bitCount = Long.bitCount( ~window ) ) < delta; ) {
			window = list.getLong( ++curr );
			delta -= bitCount;
			currentIndex += Long.SIZE - bitCount;
		}
			
		/* Note that for delta == 1 the following code is a NOP, but the test for zero is so faster that
	       it is not worth replacing with a > 1. Predecrementing won't work as delta might be zero. */
		if ( delta-- != 0 ) { 
			// Phase 1: sums by byte
			final long word = ~window;
			assert delta < Long.bitCount( word ) : delta + " >= " + Long.bitCount( word );
			long byteSums = word - ( ( word & 0xa * ONES_STEP_4 ) >>> 1 );
			byteSums = ( byteSums & 3 * ONES_STEP_4 ) + ( ( byteSums >>> 2 ) & 3 * ONES_STEP_4 );
			byteSums = ( byteSums + ( byteSums >>> 4 ) ) & 0x0f * ONES_STEP_8;
			byteSums *= ONES_STEP_8;

			// Phase 2: compare each byte sum with delta to obtain the relevant byte
			final long rankStep8 = delta * ONES_STEP_8;
			final long byteOffset = ( ( ( ( ( rankStep8 | MSBS_STEP_8 ) - byteSums ) & MSBS_STEP_8 ) >>> 7 ) * ONES_STEP_8 >>> 53 ) & ~0x7;
			
			final int byteRank = (int)( delta - ( ( ( byteSums << 8 ) >>> byteOffset ) & 0xFF ) );
			
			final int select = (int)( byteOffset + Fast.selectInByte[ (int)( word >>> byteOffset & 0xFF ) | byteRank << 8 ] );

			// We cancel up to, but not including, the target one.
			window &= -1L << select;
			currentIndex += select - delta;
		}

		final long lower = lowerBits.extract( lowerBitsStart + l * currentIndex );
		long prefixSum = getNextUpperBits() << l | lower; 
			
		for(;;) {
			if ( prefixSum >= lowerBound ) 
				return prefixSum;
			prefixSum = getNextPrefixSum();
		}
	}
	
	@Override
	public String toString() 
	{
		return this.getClass().getSimpleName() + '@' + Integer.toHexString( System.identityHashCode( this ) );
	}
}