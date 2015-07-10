package it.cnr.isti.hpclab.ef.structures;


import it.cnr.isti.hpclab.ef.EliasFano;
import it.unimi.dsi.fastutil.longs.LongBigList;
import it.unimi.dsi.util.ByteBufferLongBigList;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel.MapMode;
import java.io.FileInputStream;

import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.Pointer;
import org.terrier.structures.PostingIndex;
import org.terrier.structures.postings.IterablePosting;

public class EFInvertedIndex implements PostingIndex<BitIndexPointer>
{
	private IndexOnDisk index = null;
	private DocumentIndex doi = null;
	
	private LongBigList docidsList;
	private LongBigList freqsList;
	
	public EFInvertedIndex(IndexOnDisk index, String structureName) throws IOException 
	{
		this(index, structureName, index.getDocumentIndex());
	}

	@SuppressWarnings("resource")
	public EFInvertedIndex(IndexOnDisk index, String structureName, DocumentIndex _doi) throws IOException
	{
		this.index = index;
		this.doi = _doi;

		String byteOrderString = index.getIndexProperty(EliasFano.BYTEORDER, "");
		ByteOrder byteOrder;
		if (byteOrderString.equals("LITTLE_ENDIAN"))
			byteOrder = ByteOrder.LITTLE_ENDIAN;
		else if (byteOrderString.equals("BIG_ENDIAN"))
			byteOrder = ByteOrder.BIG_ENDIAN;
		else
			throw new RuntimeException();

		docidsList = ByteBufferLongBigList.map( new FileInputStream( index.getPath() + File.separator + index.getPrefix() + EliasFano.DOCID_EXTENSION ).getChannel(), byteOrder, MapMode.READ_ONLY );
		freqsList  = ByteBufferLongBigList.map( new FileInputStream( index.getPath() + File.separator + index.getPrefix() + EliasFano.FREQ_EXTENSION  ).getChannel(), byteOrder, MapMode.READ_ONLY );
	}
	
	@Override
	public IterablePosting getPostings(Pointer pointer) throws IOException 
	{
		EFLexiconEntry le = (EFLexiconEntry)pointer;
		int df = le.getDocumentFrequency();
		int N = index.getCollectionStatistics().getNumberOfDocuments();
		long docidOffset = le.getDocidOffset();
		long freqOffset = le.getFreqOffset();
		int F = le.getFrequency();
				
		int log2Quantum = index.getIntIndexProperty(EliasFano.LOG2QUANTUM, 0);
		if (log2Quantum == 0)
			throw new RuntimeException();
		

		IterablePosting rtr = new EFBasicIterablePosting(docidsList, freqsList, doi, df, N, F, log2Quantum, docidOffset, freqOffset);
		return rtr;
	}
	
	@Override
	public void close()
	{
	}
}