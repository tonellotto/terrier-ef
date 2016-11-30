package it.cnr.isti.hpclab.ef.structures;

import java.util.Arrays;

import org.terrier.structures.DocumentIndex;

import it.cnr.isti.hpclab.ef.util.LongWordBitReader;
import it.cnr.isti.hpclab.ef.util.PosReader;
import it.cnr.isti.hpclab.ef.util.Utils;
import it.unimi.dsi.fastutil.longs.LongBigList;

public class EFPosIterablePosting extends EFBasicIterablePosting
{
	private LongBigList posList;

	private LongWordBitReader posLongWordBitReader;
	private PosReader posReader = null;
	
	private int[] currentPositions;

	public EFPosIterablePosting()
	{	
	}

	public EFPosIterablePosting(LongBigList _docidList, LongBigList _freqList, LongBigList _posList, 
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
		
		
		this.posReader = new PosReader( posList, l, posLongWordBitReader.position(), numberOfPointers, pointerSize, upperBoundFreq, log2Quantum );
		currentPositions = null;
	}
	
	public int[] getPositions()
	{
		int numPositions = super.getFrequency();
		currentPositions = new int[numPositions];
		
		currentPositions[0] = posReader.getFirstPosition(super.freqReader.prevPrefixSum() + super.freqReader.currentIndex() - 1);
		for (int i = 1; i < numPositions; i++)
			currentPositions[i] = posReader.getNextPosition();
		return currentPositions;
	}
	
	@Override
	public String toString()
	{
		return super.toString() + "[" + Arrays.toString(currentPositions) + "]";
	}
}
