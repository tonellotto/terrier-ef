package it.cnr.isti.hpclab.ef.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * This is a cache for long (i.e., 64 bits) objects, accessible at bit level. It seems that
 * the cache must be populated first, then "rewinded", then accessed sequentially.
 * It is backed by a file {@link #spillFile} on disk accessed via Java NIO {@link #spillChannel}. 
 * Up to {@link #cacheLength} bits are held in memory, everything else on disk.
 *
 */
public final class LongWordCache implements Closeable 
{
	private static final boolean ASSERTS = true;
	/** The spill file. */
	private final File spillFile;
	/** A channel opened on {@link #spillFile}. */
	private final FileChannel spillChannel;
	
	/** A cache for longwords. Will be spilled to {@link #spillChannel} in case more than {@link #cacheLength} bits are added. */
	private final ByteBuffer cache;
	
	/** The current bit buffer. */
	private long buffer;
	/** The current number of free bits in {@link #buffer}. */
	private int free;
	
	/** The length of the cache, in <b>bits</b>. */
	private long cacheLength;
	
	/** The number of bits currently stored. */
	private long length;
	
	/** Whether {@link #spillChannel} should be repositioned at 0 <b>before usage</b>. */
	private boolean spillMustBeRewind;

	/**
	 * Creates a cache with a length of <code>cacheSize</code> length in bits. 
	 * The <code>suffix</code> is the suffix of the temporary file created to back up
	 * the cache on disk. It is deleted on exit.
	 * 
	 * @param cacheSize the length of the cache memory buffer in bits
	 * @param suffix the suffix of the temporary file backing up the cache on disk
	 * @throws IOException if something goes wrong
	 */
	@SuppressWarnings("resource")
	public LongWordCache(final int cacheSize, final String suffix) throws IOException 
	{
		spillFile = File.createTempFile(LongWordCache.class.getName(), suffix);
		spillFile.deleteOnExit();
		spillChannel = new RandomAccessFile(spillFile, "rw").getChannel();
		cache = ByteBuffer.allocateDirect(cacheSize).order(ByteOrder.nativeOrder());
		cacheLength = cacheSize * 8L; // in bits
		free = Long.SIZE;
	}

	/**
	 * Insert in cache a long <code>value</code> on <code>width</code> bits (lower positions).
	 * @param value the value to insert in cache
	 * @param width the size in bits of the value to insert
	 * @return the number of bits written
	 * @throws IOException if something goes wrong
	 */
	public int append(final long value, final int width) throws IOException 
	{
		if (ASSERTS)
			assert width == Long.SIZE || (-1L << width & value) == 0; // -1 == 0xFFFFFFFF
		buffer |= value << (Long.SIZE - free);
		length += width;

		if (width < free)
			free -= width;
		else {
			flushBuffer();

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
	 * Empty the cache
	 */
	public void clear() 
	{
		length = buffer = 0;
		free = Long.SIZE;
		cache.clear();
		spillMustBeRewind = true;
	}

	/**
	 * Close the underlying stream and delete the file
	 */
	@Override
	public void close() throws IOException 
	{
		spillChannel.close();
		spillFile.delete();
	}

	/**
	 * Return the number of bits currently stored in cache
	 * @return the number of bits currently stored
	 */
	public long length() 
	{
		return length;
	}

	/**
	 * Write an integer in unary coding
	 * @param l the integer to write
	 * @throws IOException if something goes wrong
	 */
	public void writeUnary(int l) throws IOException 
	{
		if (l >= free) {
			// Phase 1: align
			l -= free;
			length += free;
			flushBuffer();

			// Phase 2: jump over longwords
			buffer = 0;
			free = Long.SIZE;
			while (l >= Long.SIZE) {
				flushBuffer();
				l -= Long.SIZE;
				length += Long.SIZE;
			}
		}

		append(1L << l, l + 1);
	}

	/**
	 * Return the next long from the cache
	 * @return the next long from the cache 
	 * @throws IOException if something goes wrong
	 */
	public long readLong() throws IOException 
	{
		if (!cache.hasRemaining()) {
			cache.clear();
			spillChannel.read(cache);
			cache.flip();
		}
		return cache.getLong();
	}

	/**
	 * Convert the cache from writing to reading. No write operations allowed after rewind.
	 * @throws IOException if something goes wrong
	 */
	public void rewind() throws IOException 
	{
		if (free != Long.SIZE)
			cache.putLong(buffer);

		if (length > cacheLength) {
			cache.flip();
			spillChannel.write(cache);
			spillChannel.position(0);
			cache.clear();
			spillChannel.read(cache);
			cache.flip();
		} else
			cache.rewind();
	}
	
	private void flushBuffer() throws IOException 
	{
		cache.putLong(buffer);
		if (!cache.hasRemaining()) {
			if (spillMustBeRewind) {
				spillMustBeRewind = false;
				spillChannel.position(0);
			}
			cache.flip();
			spillChannel.write(cache);
			cache.clear();
		}
	}
	
	public static void main(String[] args) throws IOException
	{
		LongWordCache cache = new LongWordCache(8, "tmp");
		
		cache.append(11, Long.SIZE);
		cache.append(12, Long.SIZE);
		cache.append(13, Long.SIZE);
		cache.append(14, Long.SIZE);
		cache.append(15, Long.SIZE);
		cache.append(16, Long.SIZE);
		cache.append(17, Long.SIZE);
		cache.append(18, Long.SIZE);
		cache.append(19, Long.SIZE);
		cache.append(20, Long.SIZE);
		
		cache.append(30, Long.SIZE);
		
		cache.rewind();
		
		System.err.println(cache.readLong());
		System.err.println(cache.readLong());
		System.err.println(cache.readLong());
		System.err.println(cache.readLong());
		
		//cache.rewind();
		
		System.err.println(cache.readLong());
		System.err.println(cache.readLong());
		System.err.println(cache.readLong());
		System.err.println(cache.readLong());
		
		cache.close();
	}
}