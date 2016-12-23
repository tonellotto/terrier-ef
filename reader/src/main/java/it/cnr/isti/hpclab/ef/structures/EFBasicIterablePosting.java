package it.cnr.isti.hpclab.ef.structures;

import it.cnr.isti.hpclab.ef.util.DocidReader;
import it.cnr.isti.hpclab.ef.util.FreqReader;
import it.cnr.isti.hpclab.ef.util.LongWordBitReader;
import it.cnr.isti.hpclab.ef.util.Utils;
import it.unimi.dsi.fastutil.longs.LongBigList;

import java.io.IOException;

import org.terrier.structures.DocumentIndex;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.structures.postings.WritablePosting;

public class EFBasicIterablePosting implements IterablePosting
{
	private LongBigList docidList;
	private LongBigList freqList;
	private LongWordBitReader docidsLongWordBitReader;
	private DocumentIndex doi;
	
	protected DocidReader docidReader = null;
	protected FreqReader freqReader = null;
	protected long currentDocument;
	protected long currentFrequency;
	protected long N;
	
	public EFBasicIterablePosting()
	{	
	}

	public EFBasicIterablePosting(LongBigList _docidList, LongBigList _freqList, DocumentIndex doi, int numEntries, int upperBoundDocid, int upperBoundFreq, int log2Quantum, long docidsPosition, long freqsPosition)
	{
		this.docidList = _docidList;
		this.freqList = _freqList;
		this.doi = doi;
		this.N = upperBoundDocid;
		
		// TODO: This is probably useless, we move to docidsPosition, then we read docidsPosition, and the object is never used again...
		
		docidsLongWordBitReader = new LongWordBitReader( docidList, 0 );
		docidsLongWordBitReader.position(docidsPosition);
		
		// the number of lower bits for the EF encoding of a list of given length, upper bound and strictness.
		int l = Utils.lowerBits( numEntries + 1, upperBoundDocid, false );
		// the size in bits of forward or skip pointers to the EF encoding of a list of given length, upper bound and strictness.
		int pointerSize = Utils.pointerSize( numEntries + 1, upperBoundDocid, false, true );
		// the number of skip pointers to the EF encoding of a list of given length, upper bound and strictness.
		long numberOfPointers = Utils.numberOfPointers( numEntries + 1, upperBoundDocid, log2Quantum, false, true );

		// Reader of elements of size pointerSize
		LongWordBitReader skipPointers = new LongWordBitReader( docidList, pointerSize );
		// Reader of elements of size l
		LongWordBitReader lowerBits = new LongWordBitReader( docidList, l );

		// Where to start reading the skip pointers
		long skipPointersStart = docidsLongWordBitReader.position();
		// Where to start reading the lower bits array
		long lowerBitsStart = skipPointersStart + pointerSize * numberOfPointers;
		lowerBits.position( lowerBitsStart ); 						
				
		this.docidReader = new DocidReader( docidList, lowerBits, lowerBitsStart, l, skipPointers, skipPointersStart, numberOfPointers, pointerSize, numEntries, log2Quantum );
		currentDocument = -2;
		
		this.freqReader = new FreqReader( freqList, freqsPosition, numEntries, upperBoundFreq, log2Quantum );
		currentFrequency = 0;
	}
	
	@Override
	public int getId() 
	{
		return (int) currentDocument;
	}

	@Override
	public int getFrequency() 
	{
		return (int) currentFrequency;
	}

	@Override
	public int getDocumentLength() 
	{
		try {
			return doi.getDocumentLength((int) currentDocument);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setId(int id) 
	{
		throw new RuntimeException();
		
	}

	@Override
	public WritablePosting asWritablePosting() 
	{
		throw new RuntimeException();
	}

	@Override
	public void close() throws IOException 
	{
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
		}
		
		return (int) currentDocument;
	}

	@Override
	public boolean endOfPostings() 
	{
		return (currentDocument == IterablePosting.END_OF_LIST);
	}
	
	@Override
	public String toString()
	{
		return "(" + currentDocument + "," + currentFrequency + ")";
	}
}
