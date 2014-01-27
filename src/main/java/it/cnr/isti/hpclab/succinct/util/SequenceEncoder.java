package it.cnr.isti.hpclab.succinct.util;

import it.unimi.dsi.bits.Fast;

import java.io.Closeable;
import java.io.IOException;

/**
 * This class implements an encoder of sequences of natural numbers according to Elias Fano and can dump it to a bit file,
 * according to an internally hardcoded structure (pointers, lowers, uppers).
 */
public class SequenceEncoder implements Closeable 
{
	private static final boolean ASSERTS = true;
	/** The minimum size in bytes of a {@link LongWordCache}. */
	private static final int MIN_CACHE_SIZE = 16;

	/** The accumulator for pointers (to zeros or ones). */
	private final LongWordCache pointers;
	/** The accumulator for high bits. */
	private final LongWordCache upperBits;
	/** The accumulator for low bits. */
	private final LongWordCache lowerBits;

	/** If true, {@link #add(long)} does not accept zeroes. */
	private boolean strict;

	/** The number of lower bits. */
	private int l;
	/** A mask extracting the {@link #l} lower bits. */
	private long lowerBitsMask;
	/** The number of elements that will be added to this list. */
	private long length;
	/** The current length of the list. */
	private long currentLength;
	/** The current prefix sum (decremented by {@link #currentLength} if {@link #strict} is true). */
	private long currentPrefixSum;
	/** An upper bound to the sum of all values that will be added to the list (decremented by {@link #currentLength} if {@link #strict} is true). */
	private long correctedUpperBound;
	/** The logarithm of the indexing quantum. */
	private int log2Quantum;
	/** The indexing quantum. */
	private long quantum;
	/** The size of a pointer (the ceiling of the logarithm of {@link #maxUpperBits}). */
	private int pointerSize;
	/** The mask to decide whether to quantize. */
	private long quantumMask;
	/** Whether we should index ones or zeroes. */
	private boolean indexZeroes;
	/** If true, we are writing a ranked characteristic function. */
	private boolean ranked;
	/** The last position where a one was set. */
	private long lastOnePosition;
	/** The expected number of points. */
	private long expectedNumberOfPointers;
	/** The number of bits used for the upper-bits array. */
	public long bitsForUpperBits;
	/** The number of bits used for the lower-bits array. */
	public long bitsForLowerBits;
	/** The number of bits used for forward/skip pointers. */
	public long bitsForPointers;

	/**
	 * Constructor.
	 * @param bufferSize the size of the buffer in the file-backed caches used to perform encoding
	 * @param log2Quantum the base 2 logarithm of the quantum used to compute skip (or forward) pointer
	 * @throws IOException if something goes wrong
	 */
	public SequenceEncoder(int bufferSize, int log2Quantum) throws IOException 
	{
		// A reasonable logic to allocate space.
		bufferSize = bufferSize & -bufferSize; // Ensure power of 2.
		/*
		 * Very approximately, half of the cache for lower, half for upper, and
		 * a small fraction (8/quantum) for pointers. This will generate a much
		 * larger cache than expected if quantum is very small.
		 */
		pointers  = new LongWordCache(Math.max(MIN_CACHE_SIZE, bufferSize >>> Math.max(3, log2Quantum - 3)), "pointers");
		lowerBits = new LongWordCache(Math.max(MIN_CACHE_SIZE, bufferSize / 2), "lower");
		upperBits = new LongWordCache(Math.max(MIN_CACHE_SIZE, bufferSize / 2), "upper");
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
		return pointerSize;
	}

	/**
	 * Return the expected number of points.
	 * @return the expected number of points.
	 */
	public long numberOfPointers() 
	{
		return expectedNumberOfPointers;
	}

	/**
	 * Initialization of the encoder. Must be called before actual encoding begins.
	 * @param length the number of elements to encode
	 * @param upperBound the upper bound on the last element to encode
	 * @param strict if <code>true</code>  {@link #add(long)} does not accept zeroes.
	 * @param indexZeroes whether we should index ones or zeroes. if true, skip pointers are used; otherwise,
	 *                    forward pointers are used.
	 * @param log2Quantum the base 2 logarithm of the quantum used to compute skip (or forward) pointer
	 */
	public void init(final long length, final long upperBound, final boolean strict, final boolean indexZeroes, final int log2Quantum) 
	{
		this.indexZeroes = indexZeroes;
		this.log2Quantum = log2Quantum;
		this.length = length;
		this.strict = strict;
		quantum = 1L << log2Quantum;
		quantumMask = quantum - 1;
		pointers.clear();
		lowerBits.clear();
		upperBits.clear();
		correctedUpperBound = upperBound - (strict ? length : 0);
		final long correctedLength = length + (!strict && indexZeroes ? 1 : 0); // The length including the final terminator
		if (correctedUpperBound < 0)
			throw new IllegalArgumentException();

		currentPrefixSum = 0;
		currentLength = 0;
		lastOnePosition = -1;

		l = Utils.lowerBits(correctedLength, upperBound, strict);

		ranked = correctedLength + (upperBound >>> l) + correctedLength * l > upperBound && !strict && indexZeroes;
		if (ranked)
			l = 0;

		lowerBitsMask = (1L << l) - 1;

		pointerSize = ranked ? Fast.length(correctedLength)	: Utils.pointerSize(correctedLength, upperBound, strict, indexZeroes);
		expectedNumberOfPointers = ranked ? Math.max(0, upperBound >>> log2Quantum) : Utils.numberOfPointers(correctedLength, upperBound, log2Quantum, strict, indexZeroes);
	}

	/**
	 * Add a new natural number to the encode.
	 * @param x the natural number to add
	 * @throws IOException if something goes wrong
	 */
	public void add(final long x) throws IOException 
	{
		// System.err.println( "add(" + x + "), l = " + l + ", length = " + length );
		if (strict && x == 0)
			throw new IllegalArgumentException("Zeroes are not allowed.");
		currentPrefixSum += x - (strict ? 1 : 0);
		if (currentPrefixSum > correctedUpperBound)
			throw new IllegalArgumentException("Too large prefix sum: "	+ currentPrefixSum + " >= " + correctedUpperBound);
		if (l != 0)
			lowerBits.append(currentPrefixSum & lowerBitsMask, l);
		final long onePosition = ranked ? currentPrefixSum : (currentPrefixSum >>> l) + currentLength;

		upperBits.writeUnary((int) (onePosition - lastOnePosition - 1));

		if (ranked) {
			for (long position = lastOnePosition + quantum & -1L << log2Quantum; position <= onePosition; position += quantum)
				if (position != 0)
					pointers.append(currentLength, pointerSize);
		} else if (indexZeroes) {
			long zeroesBefore = lastOnePosition - currentLength + 1;
			for (long position = lastOnePosition + (zeroesBefore & -1L << log2Quantum) + quantum - zeroesBefore; position < onePosition; position += quantum, zeroesBefore += quantum)
				pointers.append(position + 1, pointerSize);
		} else if ((currentLength + 1 & quantumMask) == 0)
			pointers.append(onePosition + 1, pointerSize);

		lastOnePosition = onePosition;
		currentLength++;
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
		if (currentLength != length)
			throw new IllegalStateException();
		if (!strict && indexZeroes) {
			// Add last fictional document pointer equal to the number of documents.
			if (ranked) {
				if (lastOnePosition >= correctedUpperBound)
					throw new IllegalStateException("The last written pointer is " + lastOnePosition + " >= " + correctedUpperBound);
				add(correctedUpperBound - lastOnePosition);
			} else
				add(correctedUpperBound - currentPrefixSum);
		}
		if (ASSERTS)
			assert !ranked || pointers.length() / pointerSize == expectedNumberOfPointers : "Expected "	+ expectedNumberOfPointers + " pointers for ranked index, found " + pointers.length() / pointerSize;
		if (indexZeroes && pointerSize != 0)
			for (long actualPointers = pointers.length() / pointerSize; actualPointers++ < expectedNumberOfPointers;)
				pointers.append(0, pointerSize);
		if (ASSERTS)
			assert pointerSize == 0 || pointers.length() / pointerSize == expectedNumberOfPointers : "Expected " + expectedNumberOfPointers + " pointers, found " + pointers.length() / pointerSize;
		// System.err.println("pointerSize :" + pointerSize );
		bitsForPointers = lwobs.append(pointers);
		// System.err.println("lower: " + result );
		bitsForLowerBits = lwobs.append(lowerBits);
		// System.err.println("upper: " + result );
		bitsForUpperBits = lwobs.append(upperBits);
		// System.err.println("end: " + result );
		return bitsForLowerBits + bitsForUpperBits + bitsForPointers;
	}

	/** @inherited */
	@Override
	public void close() throws IOException 
	{
		pointers.close();
		upperBits.close();
		lowerBits.close();
	}
}