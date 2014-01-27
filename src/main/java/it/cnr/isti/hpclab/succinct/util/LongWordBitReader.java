package it.cnr.isti.hpclab.succinct.util;

import it.unimi.dsi.fastutil.longs.LongBigList;

public final class LongWordBitReader 
{
	private static final boolean DEBUG = false;

	/** The underlying list. */
	private final LongBigList list;
	/** The extraction width for {@link #extract()} and {@link #extract(long)}. */
	private final int l;
	/** {@link Long#SIZE} minus {@link #l}, cached. */
	private final int longSizeMinusl;
	/** The extraction mask for {@link #l} bits. */
	private final long mask;

	/** The 64-bit buffer, whose lower {@link #filled} bits contain data. */
	private long buffer;
	/** The number of lower used bits {@link #buffer}. */
	private int filled;
	/** The current position in the list. */
	private long curr;

	public LongWordBitReader(final LongBigList list, final int l) 
	{
		assert l < Long.SIZE;
		this.list = list;
		this.l = l;
		this.longSizeMinusl = Long.SIZE - l;
		mask = (1L << l) - 1;
		curr = -1;
	}

	public LongWordBitReader position(final long position) 
	{
		if (DEBUG)
			System.err.println(this + ".position(" + position + ") [buffer = " + Long.toBinaryString(buffer) + ", filled = " + filled + "]");

		buffer = list.getLong(curr = position / Long.SIZE);
		final int bitPosition = (int) (position % Long.SIZE);
		buffer >>>= bitPosition;
		filled = Long.SIZE - bitPosition;

		if (DEBUG)
			System.err.println(this + ".position() filled: " + filled + " buffer: " + Long.toBinaryString(buffer));
		return this;
	}

	public long position() 
	{
		return curr * Long.SIZE + Long.SIZE - filled;
	}

	private long extractInternal(final int width) 
	{
		if (DEBUG)
			System.err.println(this + ".extract(" + width + ") [buffer = " + Long.toBinaryString(buffer) + ", filled = " + filled + "]");

		if (width <= filled) {
			long result = buffer & (1L << width) - 1;
			filled -= width;
			buffer >>>= width;
			return result;
		} else {
			long result = buffer;
			buffer = list.getLong(++curr);

			final int remainder = width - filled;
			// Note that this WON'T WORK if remainder == Long.SIZE, but that's not going to happen.
			result |= (buffer & (1L << remainder) - 1) << filled;
			buffer >>>= remainder;
			filled = Long.SIZE - remainder;
			return result;
		}
	}

	public long extract() {
		if (DEBUG)
			System.err.println(this + ".extract() " + l + " bits [buffer = " + Long.toBinaryString(buffer) + ", filled = " + filled + "]");

		if (l <= filled) {
			final long result = buffer & mask;
			filled -= l;
			buffer >>>= l;
			return result;
		} else {
			long result = buffer;
			buffer = list.getLong(++curr);
			result |= buffer << filled & mask;
			// Note that this WON'T WORK if remainder == Long.SIZE, but that's // not going to happen.
			buffer >>>= l - filled;
			filled += longSizeMinusl;
			return result;
		}
	}

	public long extract(long position) 
	{
		if (DEBUG)
			System.err.println(this + ".extract(" + position + ") [l=" + l + "]");

		final int bitPosition = (int) (position % Long.SIZE);
		final int totalOffset = bitPosition + l;
		final long result = list.getLong(curr = position / Long.SIZE) >>> bitPosition;

		if (totalOffset <= Long.SIZE) {
			buffer = result >>> l;
			filled = Long.SIZE - totalOffset;
			return result & mask;
		}

		final long t = list.getLong(++curr);

		buffer = t >>> totalOffset;
		filled = 2 * Long.SIZE - totalOffset;

		return result | t << -bitPosition & mask;
	}

	public int readUnary() {
		if (DEBUG)
			System.err.println(this + ".readUnary() [buffer = " + Long.toBinaryString(buffer) + ", filled = " + filled + "]");

		int accumulated = 0;

		for (;;) {
			if (buffer != 0) {
				final int msb = Long.numberOfTrailingZeros(buffer);
				filled -= msb + 1;
				/* msb + 1 can be Long.SIZE, so we must break down the shift. */
				buffer >>>= msb;
				buffer >>>= 1;
				if (DEBUG)
					System.err.println(this + ".readUnary() => " + (msb + accumulated));
				return msb + accumulated;
			}
			accumulated += filled;
			buffer = list.getLong(++curr);
			filled = Long.SIZE;
		}

	}

	public long readNonZeroGamma() 
	{
		final int msb = readUnary();
		return extractInternal(msb) | (1L << msb);
	}

	public long readGamma() 
	{
		return readNonZeroGamma() - 1;
	}
}