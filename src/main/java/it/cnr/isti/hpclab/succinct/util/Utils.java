package it.cnr.isti.hpclab.succinct.util;

import it.unimi.dsi.bits.Fast;

public class Utils 
{
	/** 
	 * Returns the number of lower bits for the Elias&ndash;Fano encoding of a list of given length, upper bound and strictness.
	 * 
	 * @param length the number of elements of the list.
	 * @param upperBound an upper bound for the elements of the list.
	 * @param strict if true, the elements of the list are strictly increasing, and the
	 *               returned number of bits is for the strict representation (e.g., storing the
	 *               <var>k</var>-th element decreased by <var>k</var>).
	 * @return the number of bits for the Elias&ndash;Fano encoding of a list with the specified parameters.
	 */
	public static int lowerBits( final long length, final long upperBound, final boolean strict ) 
	{
		return length == 0 ? 0 : Math.max( 0, Fast.mostSignificantBit( ( upperBound - ( strict ? length : 0 ) ) / length ) );
	}
	
	/** 
	 * Returns the size in bits of forward or skip pointers to the Elias&ndash;Fano encoding of a list of given length, upper bound and strictness.
	 * 
	 * @param length the number of elements of the list.
	 * @param upperBound an upper bound for the elements of the list.
	 * @param strict if true, the elements of the list are strictly increasing, and the
	 *               returned number of bits is for the strict representation (e.g., storing the
	 *               <var>k</var>-th element decreased by <var>k</var>).
	 * @param indexZeroes if true, the number of bits for skip pointers is returned; otherwise,
	 *                    the number of bits for forward pointers is returned.
	 * @return the size of bits of forward or skip pointers the Elias&ndash;Fano encoding of a list with the specified parameters.
	 */
	public static int pointerSize( final long length, final long upperBound, final boolean strict, final boolean indexZeroes ) 
	{
		// Note that if we index ones it might happen that a pointer points just after the end of the bit stream.
		return Math.max(  0, Fast.ceilLog2( length + ( ( upperBound - ( strict ? length : 0 ) ) >>> lowerBits( length, upperBound, strict ) ) + ( indexZeroes ? 0 : 1 ) ) );
	}

	/** 
	 * Returns the number of forward or skip pointers to the Elias&ndash;Fano encoding of a list of given length, upper bound and strictness.
	 * 
	 * @param length the number of elements of the list.
	 * @param upperBound an upper bound for the elements of the list.
	 * @param log2Quantum the logarithm of the quantum size.
	 * @param strict if true, the elements of the list are strictly increasing, and the
	 *               returned number of bits is for the strict representation (e.g., storing the
	 *               <var>k</var>-th element decreased by <var>k</var>).
	 * @param indexZeroes if true, an upper bound on the number of skip pointers is returned; otherwise,
	 *                    the (exact) number of forward pointers is returned.
	 * @return an upper bound on the number of skip pointers or the (exact) number of forward pointers.
	 */
	public static long numberOfPointers( final long length, final long upperBound, final int log2Quantum, final boolean strict, final boolean indexZeroes ) 
	{
		if ( length == 0 ) 
			return 0;
		if ( indexZeroes ) 
			return ( ( upperBound - ( strict ? length : 0 ) ) >>> lowerBits( length, upperBound, strict ) ) >>> log2Quantum;
		return length >>> log2Quantum;
	}

	
}
