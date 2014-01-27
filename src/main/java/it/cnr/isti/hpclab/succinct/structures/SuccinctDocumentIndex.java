package it.cnr.isti.hpclab.succinct.structures;

import it.unimi.dsi.io.InputBitStream;
import it.unimi.dsi.io.OutputBitStream;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.DocumentIndexEntry;
import org.terrier.structures.FSADocumentIndex;
import org.terrier.structures.Index;

public class SuccinctDocumentIndex implements DocumentIndex
{
	private static Logger LOGGER = Logger.getLogger( SuccinctDocumentIndex.class );
	
	private int[] docLengths = null;
	
	public SuccinctDocumentIndex(final Index index) throws IOException
	{
		this(index.getPath() + "/" + index.getPrefix() + ".sizes", index.getCollectionStatistics().getNumberOfDocuments());
	}
	
	public SuccinctDocumentIndex(final String path, final int size) throws IOException
	{
		docLengths = new int[size];
		final InputBitStream in = new InputBitStream( new FileInputStream( path ), false );
		LOGGER.debug( "Loading document lengths..." );
		in.readGammas( docLengths, docLengths.length );		  
		LOGGER.debug( "Completed." );
		in.close();
	}
	
	@Override
	public DocumentIndexEntry getDocumentEntry(int docid) throws IOException 
	{
		throw new RuntimeException("");
	}

	@Override
	public int getDocumentLength(int docid) throws IOException 
	{
		return docLengths[docid];
	}

	@Override
	public int getNumberOfDocuments() 
	{
		return docLengths.length;
	}

	public static void write(final FSADocumentIndex index, final String path) throws IOException
	{
		final OutputBitStream out = new OutputBitStream( new FileOutputStream(path));
		for (int i = 0; i < index.getNumberOfDocuments(); i++)
			out.writeGamma(index.getDocumentLength(i));
		out.close();
	}
	
	public static void main(String[] args) throws IOException
	{
		final int numDocs = 1692096;
		DocumentIndex docIndex = new SuccinctDocumentIndex("/Users/khast/index-mg4j/wt10g-text.sizes", numDocs);
		
		for (int i = 0; i < numDocs; i++)
			System.err.println(docIndex.getDocumentLength(i));
	}
}
