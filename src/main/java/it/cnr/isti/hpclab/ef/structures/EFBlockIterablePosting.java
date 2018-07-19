package it.cnr.isti.hpclab.ef.structures;

import java.io.IOException;
import java.util.Arrays;

import org.terrier.structures.DocumentIndex;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.IterablePosting;

import it.cnr.isti.hpclab.ef.util.LongWordBitReader;
import it.cnr.isti.hpclab.ef.util.PositionReader;
import it.cnr.isti.hpclab.ef.util.Utils;
import it.unimi.dsi.fastutil.longs.LongBigList;

/**
 * Elias-Fano implementation of an block iterable posting, i.e., a posting cursor over a posting list with positional information stored in postings.
 */

public class EFBlockIterablePosting extends EFBasicIterablePosting implements BlockPosting
{
	private LongBigList posList;

	private LongWordBitReader posLongWordBitReader;
	private PositionReader posReader = null;
	
	private int[] currentPositions;
	private boolean positionsDecoded;

	/** 
	 * Create an empty EFBlockIterablePosting.
	 */
	public EFBlockIterablePosting()
	{	
	}

	/**
	 * Create a EFBlockIterablePosting object.
	 * 
	 * @param _docidList the Elias-Fano compressed list view to access to read docids
	 * @param _freqList the Elias-Fano compressed list view to access to read frequencies
	 * @param _posList the Elias-Fano compressed list view to access to read positional information
	 * @param doi the document index to use to read document lengths
	 * @param numEntries number of postings in the posting list
	 * @param upperBoundDocid upper bound on the docids
	 * @param upperBoundFreq upper bound on the frequency
	 * @param upperBoundFreq upper bound on the positional information
	 * @param log2Quantum the quantum used to encode forward (skip) pointers
	 * @param docidsPosition the initial bit offset in the docids file of this posting list
	 * @param freqsPosition the initial bit offset in the freq file of this posting list
	 * @param posPosition the initial bit offset in the position file of this posting list
	 */

	public EFBlockIterablePosting(final LongBigList _docidList, final LongBigList _freqList, final LongBigList _posList, 
								  final DocumentIndex doi, final int numEntries, 
								  final int upperBoundDocid, final int upperBoundFreq, final long upperBoundPos,
								  final int log2Quantum, 
								  final long docidsPosition, final long freqsPosition, final long posPosition)
	{
		super(_docidList, _freqList, doi, numEntries, upperBoundDocid, upperBoundFreq, log2Quantum, docidsPosition, freqsPosition);
		this.posList = _posList;
		
		posLongWordBitReader = new LongWordBitReader( posList, 0 );
		posLongWordBitReader.position(posPosition);
				
		// the number of lower bits for the EF encoding of a list of given length, upper bound and strictness.
		int l = (int) posLongWordBitReader.readGamma();
		// the number of skip pointers to the EF encoding of a list of given length, upper bound and strictness.
		long numberOfPointers = Utils.numberOfPointers( upperBoundFreq, -1, log2Quantum, true, false );
		// the size in bits of forward or skip pointers to the EF encoding of a list of given length, upper bound and strictness.
		int pointerSize = (numberOfPointers == 0 ? -1 : (int) posLongWordBitReader.readNonZeroGamma());
		
		
		this.posReader = new PositionReader( posList, l, posLongWordBitReader.position(), numberOfPointers, pointerSize, upperBoundFreq, log2Quantum );
		currentPositions = null;
	}
	
	/** {@inheritDoc} */
	@Override
	public int[] getPositions()
	{
		if (!positionsDecoded) {
			int numPositions = super.getFrequency();
			currentPositions = new int[numPositions];
		
			currentPositions[0] = posReader.getFirstPosition(super.freqReader.prevPrefixSum() + super.freqReader.currentIndex() - 1);
			for (int i = 1; i < numPositions; i++)
				currentPositions[i] = posReader.getNextPosition();
			positionsDecoded = true;
		}
		return currentPositions;
	}
	
	/** {@inheritDoc} */
	@Override
	public int next() throws IOException 
	{
		if ( currentDocument == IterablePosting.END_OF_LIST ) 
			return IterablePosting.END_OF_LIST;

		if ( ( currentDocument = docidReader.getNextPrefixSum() ) >= N ) {
			currentDocument = IterablePosting.END_OF_LIST;
		} else {
			currentFrequency = freqReader.getLong( docidReader.currentIndex - 1 );
			positionsDecoded = false;
		}
		
		return (int) currentDocument;	
	}

	/** {@inheritDoc} */
	@Override
	public int next(int targetId) throws IOException 
	{
		if ( targetId >= N ) 
			return (int) (currentDocument = IterablePosting.END_OF_LIST);

		if ( currentDocument >= targetId ) 
			return (int) currentDocument;

		if ( ( currentDocument = docidReader.skipTo( targetId ) ) >= N ) {
			currentDocument = IterablePosting.END_OF_LIST;
		}  else {
			currentFrequency = freqReader.getLong( docidReader.currentIndex - 1 );
			positionsDecoded = false;
		}
		
		return (int) currentDocument;
	}

	/** {@inheritDoc} */
	@Override
	public String toString()
	{
		return super.toString() + "[" + Arrays.toString(currentPositions) + "]";
	}
}
