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
 * The original source code is it.unimi.di.big.mg4j.index.QuasiSuccinctIndexWriter class (Accumulator)
 * 
 * http://mg4j.di.unimi.it/docs-big/it/unimi/di/big/mg4j/index/QuasiSuccinctIndexWriter.html
 * 
 * being part of
 *  		 
 * MG4J: Managing Gigabytes for Java (big)
 *
 * Copyright (C) 2012 Sebastiano Vigna 
 */
package it.cnr.isti.hpclab.ef.util;

import java.io.Closeable;
import java.io.IOException;

/**
 * This class implements an encoder of sequences of natural numbers according to Elias-Fano and can dump it to a bit file,
 * according to an internally hardcoded structure (pointers, lowers, uppers).
 * It indexes zeroes (to store docid 0), i.e., {@link #add(long)} does accept zeroes, and indexes zeroes, i.e., skip pointers are used.
 */
@Deprecated
public class DocidEncoder implements Closeable 
{
	/** The minimum size in bytes of a {@link LongWordCache}. */
	private static final int MIN_CACHE_SIZE = 16;

	/** The accumulator for pointers (to zeroes). */
	private final LongWordCache pointers;
	/** The accumulator for high bits. */
	private final LongWordCache upper_bits;
	/** The accumulator for low bits. */
	private final LongWordCache lower_bits;

	/** The number of lower bits. */
	private int l;
	/** A mask extracting the {@link #l} lower bits. */
	private long lowerBitsMask;
	
	/** The number of elements that will be added to this list. */
	private long length;
	/** The current length of the list. */
	private long current_length;
	/** The current prefix sum (decremented by {@link #current_length} if {@link #strict} is true). */
	private long current_prefix_sum;
	/** An upper bound to the sum of all values that will be added to the list. */
	private long upper_bound;
	/** The logarithm of the indexing quantum. */
	private int log2_quantum;
	/** The indexing quantum. */
	private long quantum;
	/** The size of a pointer (the ceiling of the logarithm of {@link #maxUpperBits}). */
	private int pointer_size;

	/** The last position where a one was set. */
	private long last_one_position;
	/** The expected number of points. */
	private long expected_number_of_pointers;
	
	/** The number of bits used for the upper-bits array. */
	public long bits_for_upper_bits;
	/** The number of bits used for the lower-bits array. */
	public long bits_for_lower_bits;
	/** The number of bits used for forward/skip pointers. */
	public long bits_for_pointers;

	/**
	 * Constructor.
	 * @param bufferSize the size of the buffer in the file-backed caches used to perform encoding
	 * @param log2Quantum the base 2 logarithm of the quantum used to compute skip (or forward) pointer
	 * @throws IOException if something goes wrong
	 */
	public DocidEncoder(final int bufferSize, final int log2Quantum) throws IOException 
	{
		int bufferSize_ = bufferSize & -bufferSize; // Ensure power of 2.
		/*
		 * Very approximately, half of the cache for lower, half for upper, and
		 * a small fraction (8/quantum) for pointers. This will generate a much
		 * larger cache than expected if quantum is very small.
		 */
		pointers   = new LongWordCache(Math.max(MIN_CACHE_SIZE, bufferSize_ >>> Math.max(3, log2Quantum - 3)), "pointers");
		lower_bits = new LongWordCache(Math.max(MIN_CACHE_SIZE, bufferSize_ / 2), "lower");
		upper_bits = new LongWordCache(Math.max(MIN_CACHE_SIZE, bufferSize_ / 2), "upper");
	}

	/**
	 * Return the number of lower bits used in encoding so far.
	 * @return the number of lower bits used in encoding so far
	 */
	public int lowerBits() 
	{
		return l;
	}

	/**
	 * Return the size of a pointer (the ceiling of the logarithm of {@link #maxUpperBits}).
	 * @return The size of a pointer (the ceiling of the logarithm of {@link #maxUpperBits})
	 */
	public int pointerSize() 
	{
		return pointer_size;
	}

	/**
	 * Return the expected number of points.
	 * @return the expected number of points.
	 */
	public long numberOfPointers() 
	{
		return expected_number_of_pointers;
	}

	/**
	 * Initialization of the encoder. Must be called before actual encoding begins.
	 * @param length the number of elements to encode
	 * @param upperBound the upper bound on the last element to encode
	 * @param log2Quantum the base 2 logarithm of the quantum used to compute skip (or forward) pointer
	 */	
	public void init(final long length, final long upper_bound, final int log2Quantum) 
	{
		if (upper_bound < 0)
			throw new IllegalArgumentException();

		this.length      = length;
		this.upper_bound = upper_bound;
		
		this.log2_quantum = log2Quantum;
		this.quantum     = 1L << log2Quantum;

		pointers.clear();
		lower_bits.clear();
		upper_bits.clear();
		
		current_prefix_sum = 0;
		current_length    = 0;
		last_one_position  = -1;

		final long corrected_length = length + 1; // The length including the final terminator
		
		l = EFUtils.lowerBits(corrected_length, upper_bound, false);
		pointer_size = EFUtils.pointerSize(corrected_length, upper_bound, false, true);
		expected_number_of_pointers = EFUtils.numberOfPointers(corrected_length, upper_bound, log2Quantum, false, true);
		
		lowerBitsMask = (1L << l) - 1;
	}

	/**
	 * Add a new natural number to the encode.
	 * @param x the natural number to add
	 * @throws IOException if something goes wrong
	 */
	public void add(final long x) throws IOException 
	{
		current_prefix_sum += x;
		if (current_prefix_sum > upper_bound)
			throw new IllegalArgumentException("Too large prefix sum: "	+ current_prefix_sum + " >= " + upper_bound);
		if (l != 0)
			lower_bits.append(current_prefix_sum & lowerBitsMask, l);
		final long onePosition = (current_prefix_sum >>> l) + current_length;

		upper_bits.writeUnary((int) (onePosition - last_one_position - 1));

		long zeroesBefore = last_one_position - current_length + 1;
		for (long position = last_one_position + (zeroesBefore & -1L << log2_quantum) + quantum - zeroesBefore; position < onePosition; position += quantum, zeroesBefore += quantum)
			pointers.append(position + 1, pointer_size);

		last_one_position = onePosition;
		current_length++;
	}
	
	/**
	 * Dump the complete encoded sequence to a bit output stream.
	 * Could add last fictional document pointer equal to the number of documents.
	 * 
	 * @param lwobs the output bit stream where to dump
	 * @return the number of dumped bits
	 * @throws IOException if something goes wrong
	 */
	public long dump(final LongWordBitWriter lwobs) throws IOException 
	{
		if (current_length != length)
			throw new IllegalStateException();
		// Add last fictional document pointer equal to the number of documents.
		add(upper_bound - current_prefix_sum);
		if (pointer_size != 0)
			for (long actualPointers = pointers.length() / pointer_size; actualPointers++ < expected_number_of_pointers;)
				pointers.append(0, pointer_size);

		bits_for_pointers  = lwobs.append(pointers);
		bits_for_lower_bits = lwobs.append(lower_bits);
		bits_for_upper_bits = lwobs.append(upper_bits);

		return bits_for_lower_bits + bits_for_upper_bits + bits_for_pointers;
	}

	/** @inherited */
	@Override
	public void close() throws IOException 
	{
		pointers.close();
		upper_bits.close();
		lower_bits.close();
	}
}