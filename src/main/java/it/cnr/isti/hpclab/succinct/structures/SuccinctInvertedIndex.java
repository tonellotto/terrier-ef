package it.cnr.isti.hpclab.succinct.structures;


import it.unimi.dsi.fastutil.longs.LongBigList;
import it.unimi.dsi.util.ByteBufferLongBigList;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel.MapMode;
import java.io.FileInputStream;

import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.PostingIndex;
import org.terrier.structures.postings.IterablePosting;

public class SuccinctInvertedIndex implements PostingIndex<BitIndexPointer>
{
	private Index index = null;
	private DocumentIndex doi = null;
	
	private LongBigList docidsList;
	private LongBigList freqsList;
	
	public SuccinctInvertedIndex(Index index, String structureName) throws IOException 
	{
		this(index, structureName, index.getDocumentIndex());
	}

	@SuppressWarnings("resource")
	public SuccinctInvertedIndex(Index index, String structureName, DocumentIndex _doi) throws IOException
	{
		this.index = index;
		this.doi = _doi;

		String byteOrderString = index.getIndexProperty("ByteOrder", "");
		ByteOrder byteOrder;
		if (byteOrderString.equals("LITTLE_ENDIAN"))
			byteOrder = ByteOrder.LITTLE_ENDIAN;
		else if (byteOrderString.equals("BIG_ENDIAN"))
			byteOrder = ByteOrder.BIG_ENDIAN;
		else
			throw new RuntimeException();

		docidsList = ByteBufferLongBigList.map( new FileInputStream( index.getPath() + File.separator + index.getPrefix() + ".docids" ).getChannel(), byteOrder, MapMode.READ_ONLY );
		freqsList  = ByteBufferLongBigList.map( new FileInputStream( index.getPath() + File.separator + index.getPrefix() + ".freqs" ).getChannel(), byteOrder, MapMode.READ_ONLY );

	}
	
	public IterablePosting getPostings(BitIndexPointer pointer) throws IOException 
	{
		int df = ((SuccinctLexiconEntry)pointer).getDocumentFrequency();
		int N = index.getCollectionStatistics().getNumberOfDocuments();
		long docidOffset = ((SuccinctLexiconEntry)pointer).getDocidOffset();
		long freqOffset = ((SuccinctLexiconEntry)pointer).getFreqOffset();
		int F = ((SuccinctLexiconEntry)pointer).getFrequency();
		
		String byteOrderString = index.getIndexProperty("ByteOrder", "");
		ByteOrder byteOrder;
		if (byteOrderString.equals("LITTLE_ENDIAN"))
			byteOrder = ByteOrder.LITTLE_ENDIAN;
		else if (byteOrderString.equals("BIG_ENDIAN"))
			byteOrder = ByteOrder.BIG_ENDIAN;
		else
			throw new RuntimeException();
		
		
		int log2Quantum = index.getIntIndexProperty("log2Quantum", 0);
		if (log2Quantum == 0)
			throw new RuntimeException();
		

		IterablePosting rtr = new SuccinctBasicIterablePosting(docidsList, freqsList, doi, df, N, F, log2Quantum, docidOffset, freqOffset);
		//IterablePosting rtr = new SuccinctBasicIterablePosting(index, byteOrder, doi, df, N, F, log2Quantum, docidOffset, freqOffset);
		return rtr;
	}
	
	@Override
	public void close()
	{
		
	}
}
