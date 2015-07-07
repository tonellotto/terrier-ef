package it.cnr.isti.hpclab.succinct.structures;

import it.cnr.isti.hpclab.succinct.util.DocidReader;
import it.cnr.isti.hpclab.succinct.util.FreqReader;
import it.cnr.isti.hpclab.succinct.util.LongWordBitReader;
import it.cnr.isti.hpclab.succinct.util.Utils;
import it.unimi.dsi.fastutil.longs.LongBigList;
// import it.unimi.dsi.util.ByteBufferLongBigList;

import java.io.IOException;

import org.terrier.structures.DocumentIndex;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.structures.postings.WritablePosting;

public class SuccinctBasicIterablePosting implements IterablePosting
{
	private LongBigList docidList;
	private LongBigList freqList;
	private LongWordBitReader docidsLongWordBitReader;
	private DocumentIndex doi;
	
	private DocidReader docidReader = null;
	private FreqReader freqReader = null;
	private long currentDocument;
	private long currentFrequency;
	private long N;
	
	//private FileInputStream dis, fis;
	
	public SuccinctBasicIterablePosting()
	{	
	}
	/*
	public SuccinctBasicIterablePosting(Index index, ByteOrder byteOrder, DocumentIndex doi, int numEntries, int upperBoundDocid, int upperBoundFreq, int log2Quantum, long docidsPosition, long freqsPosition) throws IOException
	{
		dis = new FileInputStream( index.getPath() + File.separator + index.getPrefix() + ".docids" );
		fis = new FileInputStream( index.getPath() + File.separator + index.getPrefix() + ".freqs" );
		
		this.docidList = ByteBufferLongBigList.map( dis.getChannel(), byteOrder, MapMode.READ_ONLY );
		this.freqList  = ByteBufferLongBigList.map( fis.getChannel(), byteOrder, MapMode.READ_ONLY );
	}
	*/
	public SuccinctBasicIterablePosting(LongBigList _docidList, LongBigList _freqList, DocumentIndex doi, int numEntries, int upperBoundDocid, int upperBoundFreq, int log2Quantum, long docidsPosition, long freqsPosition)
	{
		this.docidList = _docidList;
		this.freqList = _freqList;
		this.doi = doi;
		this.N = upperBoundDocid;
		
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
		//dis.close();
		//fis.close();
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

	@Override
	public long getCurrentAddress() 
	{
		throw new RuntimeException();
	}
}
