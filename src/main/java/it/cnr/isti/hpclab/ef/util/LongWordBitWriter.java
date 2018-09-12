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

/*
 * The original source code is it.unimi.di.big.mg4j.index.QuasiSuccinctIndexWriter class (LongWordOutputBitStream)
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

import java.io.IOException;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.nio.channels.WritableByteChannel;

import it.unimi.dsi.bits.Fast;

/**
 * This class implements a bit-oriented output stream implemented through long integers. 
 */
public final class LongWordBitWriter 
{
	private static final int BUFFER_SIZE = 64 * 1024;

	/** The 64-bit buffer, whose upper {@link #free} bits do not contain data. */
	private long buffer;
	/** The number of upper free bits in {@link #buffer} (strictly positive). */
	private int free;

	/** The Java NIO buffer used to write with prescribed endianess. */
	private ByteBuffer byteBuffer;
	/** The output channel. */
	private WritableByteChannel writableByteChannel;
	
	/**
	 * Constructor.
	 * 
	 * @param writableByteChannel the channel where to write.
	 * @param byteOrder the prescribed endianess for writing.
	 */
	public LongWordBitWriter(final WritableByteChannel writableByteChannel, final ByteOrder byteOrder) 
	{
		this.writableByteChannel = writableByteChannel;
		byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE).order(byteOrder);
		free = Long.SIZE;
	}

	/**
	 * Writes the <code>width</code> less significant bits of <code>value</code> long in the output stream.
	 * @param value the long containing the value to write
	 * @param width the lowest bits to read from the long
	 * @return the number of bits written
	 * @throws IOException when something goes wrong
	 */
	public int append(final long value, final int width) throws IOException 
	{		
		buffer |= value << (Long.SIZE - free);

		if (width < free)
			free -= width;
		else {
			byteBuffer.putLong(buffer); // filled
			if (!byteBuffer.hasRemaining()) {
				((Buffer)byteBuffer).flip();
				writableByteChannel.write(byteBuffer);
				((Buffer)byteBuffer).clear();
			}

			if (width == free) {
				buffer = 0;
				free = Long.SIZE;
			} else {
				// free < Long.SIZE
				buffer = value >>> free;
				free = Long.SIZE - width + free; // width > free
			}
		}
		return width;
	}

	/**
	 * Writes the <code>width</code> bits read from the <code>value</code> array of longs in the output stream.
	 * @param value the long array containing the values to write
	 * @param width the number of bits to read from the long array
	 * @return the number of bits written
	 * @throws IOException when something goes wrong
	 */
	public long append(final long[] value, final long length) throws IOException 
	{
		long l = length;
		for (int i = 0; l > 0; i++) {
			final int width = (int) Math.min(l, Long.SIZE);
			append(value[i], width);
			l -= width;
		}
		return length;
	}

	/**
	 * Writes the bits read from the <code>value</code> cache of longs in the output stream.
	 * @param value the cache of longs containing the values to write
	 * @return the number of bits written
	 * @throws IOException when something goes wrong
	 */
	public long append(final LongWordCache cache) throws IOException 
	{
		long l = cache.length();
		cache.rewind();
		while (l > 0) {
			final int width = (int) Math.min(l, Long.SIZE);
			append(cache.readLong(), width);
			l -= width;
		}
		return cache.length();
	}
	
	public void close() throws IOException 
	{
		byteBuffer.putLong(buffer);
		((Buffer)byteBuffer).flip();
		writableByteChannel.write(byteBuffer);
		writableByteChannel.close();
	}
	
	// These methods are here for positions
	
	public int writeNonZeroGamma(long value) throws IOException 
	{
		if (value <= 0)
			throw new IllegalArgumentException("The argument " + value + " is not strictly positive.");
		final int msb = Fast.mostSignificantBit(value);
		final long unary = 1L << msb;
		append(unary, msb + 1);
		append(value ^ unary, msb);
		return 2 * msb + 1;
	}

	public int writeGamma(long value) throws IOException 
	{
		if (value < 0)
			throw new IllegalArgumentException("The argument " + value + " is negative.");
		return writeNonZeroGamma(value + 1);
	}
}