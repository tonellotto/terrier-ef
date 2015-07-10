package it.cnr.isti.hpclab.ef.structures;

import it.cnr.isti.hpclab.ef.EliasFano;
import it.unimi.dsi.io.InputBitStream;
import it.unimi.dsi.io.OutputBitStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.DocumentIndexEntry;
//import org.terrier.structures.FSADocumentIndex;
import org.terrier.structures.IndexOnDisk;

public class EFDocumentIndex implements DocumentIndex
{
	private static Logger LOGGER = Logger.getLogger( EFDocumentIndex.class );
	
	private int[] docLengths = null;
	
	public EFDocumentIndex(final IndexOnDisk index) throws IOException
	{
		this(index.getPath() + File.separator + index.getPrefix() + EliasFano.SIZE_EXTENSION, index.getCollectionStatistics().getNumberOfDocuments());
	}
	
	public EFDocumentIndex(final String path, final int size) throws IOException
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

	public static void write(final DocumentIndex index, final String path) throws IOException
	{
		final OutputBitStream out = new OutputBitStream( new FileOutputStream(path));
		for (int i = 0; i < index.getNumberOfDocuments(); i++)
			out.writeGamma(index.getDocumentLength(i));
		out.close();
	}
}
