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

public class EFBlockIterablePosting extends EFBasicIterablePosting implements BlockPosting
{
	private LongBigList posList;

	private LongWordBitReader posLongWordBitReader;
	private PositionReader posReader = null;
	
	private int[] currentPositions;
	private boolean positionsDecoded;

	public EFBlockIterablePosting()
	{	
	}

	public EFBlockIterablePosting(LongBigList _docidList, LongBigList _freqList, LongBigList _posList, 
							    DocumentIndex doi, int numEntries, 
							    int upperBoundDocid, int upperBoundFreq, long upperBoundPos,
							    int log2Quantum, 
							    long docidsPosition, long freqsPosition, long posPosition)
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

	@Override
	public String toString()
	{
		return super.toString() + "[" + Arrays.toString(currentPositions) + "]";
	}
}
