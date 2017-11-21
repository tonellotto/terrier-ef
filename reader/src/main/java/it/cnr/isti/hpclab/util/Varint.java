package it.cnr.isti.hpclab.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.google.common.base.Preconditions;

/**
 * Encodes signed and unsigned values using a common variable-length scheme, found for example in
 * <a href="http://code.google.com/apis/protocolbuffers/docs/encoding.html"> Google's Protocol Buffers</a>.
 * It uses fewer bytes to encode smaller values, but will use slightly more bytes to encode large values.
 * <br>
 * Signed values are further encoded using so-called zig-zag encoding in order to make them "compatible" with variable-length encoding.
 */
public final class Varint 
{
	private Varint() 
	{
	}

	/**
	 * Encodes a value using the variable-length encoding from
	 * <a href="http://code.google.com/apis/protocolbuffers/docs/encoding.html">
	 * Google Protocol Buffers</a>. It uses zig-zag encoding to efficiently
	 * encode signed values. If values are known to be nonnegative,
	 * {@link #writeUnsignedVarLong(long, java.io.DataOutput)} should be used.
	 *
	 * @param value value to encode
	 * @param out to write bytes to
	 * @throws java.io.IOException if {@link java.io.DataOutput} throws {@link java.io.IOException}
	 */
	public static void writeSignedLong(long value, DataOutput out) throws IOException 
	{
		// Great trick from http://code.google.com/apis/protocolbuffers/docs/encoding.html#types
		writeUnsignedLong((value << 1) ^ (value >> 63), out);
	}

	/**
	 * Encodes a value using the variable-length encoding from
	 * <a href="http://code.google.com/apis/protocolbuffers/docs/encoding.html">
	 * Google Protocol Buffers</a>. Zig-zag is not used, so input must not be negative.
	 * If values can be negative, use {@link #writeSignedLong(long, java.io.DataOutput)}
	 * instead. This method treats negative input as like a large unsigned value.
	 *
	 * @param value value to encode
	 * @param out to write bytes to
	 * @throws java.io.IOException if {@link java.io.DataOutput} throws {@link java.io.IOException}
	 */
	public static void writeUnsignedLong(long value, DataOutput out) throws IOException 
	{
		while ((value & 0xFFFFFFFFFFFFFF80L) != 0L) {
			out.writeByte(((int) value & 0x7F) | 0x80);
			value >>>= 7;
		}
		out.writeByte((int) value & 0x7F);
	}

	/**
	 * @see #writeSignedLong(long, java.io.DataOutput)
	 */
	public static void writeSignedInt(int value, DataOutput out) throws IOException 
	{
		// Great trick from http://code.google.com/apis/protocolbuffers/docs/encoding.html#types
		writeUnsignedInt((value << 1) ^ (value >> 31), out);
	}

	/**
	 * @see #writeUnsignedLong(long, java.io.DataOutput)
	 */
	public static void writeUnsignedInt(int value, DataOutput out) throws IOException 
	{
		while ((value & 0xFFFFFF80) != 0L) {
			out.writeByte((value & 0x7F) | 0x80);
			value >>>= 7;
		}
		out.writeByte(value & 0x7F);
	}

	/**
	 * @param in to read bytes from
	 * @return decode value
	 * @throws java.io.IOException if {@link java.io.DataInput} throws {@link java.io.IOException}
	 * @throws IllegalArgumentException if variable-length value does not terminate after 9 bytes have been read
	 * @see #writeSignedLong(long, java.io.DataOutput)
	 */
	public static long readSignedLong(DataInput in) throws IOException 
	{
		long raw = readUnsignedLong(in);
		// This undoes the trick in writeSignedVarLong()
		long temp = (((raw << 63) >> 63) ^ raw) >> 1;
		// This extra step lets us deal with the largest signed values by treating
		// negative results from read unsigned methods as like unsigned values
		// Must re-flip the top bit if the original read value had it set.
		return temp ^ (raw & (1L << 63));
	}

	/**
	 * @param in to read bytes from
	 * @return decode value
	 * @throws java.io.IOException if {@link java.io.DataInput} throws {@link java.io.IOException}
	 * @throws IllegalArgumentException if variable-length value does not terminate after 9 bytes have been read
	 * @see #writeUnsignedLong(long, java.io.DataOutput)
	 */
	public static long readUnsignedLong(DataInput in) throws IOException 
	{
		long value = 0L;
		int i = 0;
		long b;
		while (((b = in.readByte()) & 0x80L) != 0) {
			value |= (b & 0x7F) << i;
			i += 7;
			Preconditions.checkArgument(i <= 63, "Variable length quantity is too long (must be <= 63)");
		}
		return value | (b << i);
	}

	/**
	 * @throws IllegalArgumentException if variable-length value does not terminate after 5 bytes have been read
	 * @throws java.io.IOException if {@link java.io.DataInput} throws {@link java.io.IOException}
	 * @see #readSignedLong(java.io.DataInput)
	 */
	public static int readSignedInt(DataInput in) throws IOException 
	{
		int raw = readUnsignedInt(in);
		// This undoes the trick in writeSignedVarInt()
		int temp = (((raw << 31) >> 31) ^ raw) >> 1;
		// This extra step lets us deal with the largest signed values by treating
		// negative results from read unsigned methods as like unsigned values.
		// Must re-flip the top bit if the original read value had it set.
		return temp ^ (raw & (1 << 31));
	}

	/**
	 * @throws IllegalArgumentException if variable-length value does not terminate after 5 bytes have been read
	 * @throws java.io.IOException if {@link java.io.DataInput} throws {@link java.io.IOException}
	 * @see #readUnsignedLong(java.io.DataInput)
	 */
	public static int readUnsignedInt(DataInput in) throws IOException 
	{
		int value = 0;
		int i = 0;
		int b;
		while (((b = in.readByte()) & 0x80) != 0) {
			value |= (b & 0x7F) << i;
			i += 7;
			Preconditions.checkArgument(i <= 35, "Variable length quantity is too long (must be <= 35)");
		}
		return value | (b << i);
	}
}