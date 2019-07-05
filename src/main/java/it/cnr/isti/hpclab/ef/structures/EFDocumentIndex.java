/*
 * Elias-Fano compression for Terrier 5
 *
 * Copyright (C) 2018-2020 Nicola Tonellotto 
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */
package it.cnr.isti.hpclab.ef.structures;

import it.cnr.isti.hpclab.ef.EliasFano;
import it.unimi.dsi.io.InputBitStream;
import it.unimi.dsi.io.OutputBitStream;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.terrier.structures.BitFilePosition;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.DocumentIndexEntry;
import org.terrier.structures.Index;
//import org.terrier.structures.FSADocumentIndex;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.Pointer;

/**
 * New implementation of the default Terrier document index.
 * It is a lightweight implementation, where only document lengths are stored, 
 * and loaded uncompressed in main memory at construction time.
 * This new document index is automatically generated by the Elias-Fano index conversion processes.
 */
public class EFDocumentIndex implements DocumentIndex
{
	private static Logger LOGGER = Logger.getLogger( EFDocumentIndex.class );
	
	private final int[] docLengths;
	
	/**
	 * Constructor.
	 * @param index the Elias-Fano index containing the document index
	 * @throws IOException is something goes wrong in opening/accessing/closing the document index file
	 */
	public EFDocumentIndex(final IndexOnDisk index) throws IOException
	{
		this(index.getPath() + File.separator + index.getPrefix() + EliasFano.SIZE_EXTENSION, index.getCollectionStatistics().getNumberOfDocuments());
	}
	
	/**
	 * Constructor
	 * @param path the file path of the Elias-Fano index containing the document index
	 * @param size the number of documents to read
	 * @throws IOException is something goes wrong in opening/accessing/closing the document index file
	 */
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
	public DocumentIndexEntry getDocumentEntry(final int docid) throws IOException 
	{
		return new EFDocumentIndexEntry(docid);
	}

	/** {@inheritDoc} */
	@Override
	public int getDocumentLength(final int docid) throws IOException 
	{
		return docLengths[docid];
	}

	/** {@inheritDoc} */
	@Override
	public int getNumberOfDocuments() 
	{
		return docLengths.length;
	}

	/**
	 * Static methods to store an int array in a gamma-encoded sequence on file
	 * @param index the document index containing the integer to encode and write to file.
	 * @param path the destination file
	 * @throws IOException is something goes wrong in opening/accessing/closing the document index file 
	 */
	public static void write(final DocumentIndex index, final String path) throws IOException
	{
		final OutputBitStream out = new OutputBitStream( new FileOutputStream(path));
		for (int i = 0; i < index.getNumberOfDocuments(); i++)
			out.writeGamma(index.getDocumentLength(i));
		out.close();
	}
	
	public static void main(String[] args) throws IOException
	{
		Index.setIndexLoadingProfileAsRetrieval(false);
		Index idx = Index.createIndex();
		if (idx == null) {
			LOGGER.error("No such index : "+ Index.getLastIndexLoadError());				
		} else {
			DocumentIndex di = new EFDocumentIndex((IndexOnDisk) idx);
			for (int i = 0; i < di.getNumberOfDocuments(); i++) {
				System.out.println(di.getDocumentLength(i));
			}
		}
	}
	
	public static class Entry extends DocumentIndexEntry
	{
		private final int doc_len;
		
		public Entry(final int dl)
		{
			this.doc_len = dl;
		}
		
		@Override
		public void setBitIndexPointer(BitIndexPointer pointer) 
		{
			throw new UnsupportedOperationException("Should not be invoked");
		}

		@Override
		public void setOffset(BitFilePosition pos) 
		{
			throw new UnsupportedOperationException("Should not be invoked");
		}

		@Override
		public void readFields(DataInput arg0) throws IOException 
		{
			throw new UnsupportedOperationException("Should not be invoked");
		}

		@Override
		public void write(DataOutput arg0) throws IOException 
		{
			throw new UnsupportedOperationException("Should not be invoked");	
		}

		@Override
		public void setNumberOfEntries(int n) 
		{
			throw new UnsupportedOperationException("Should not be invoked");
		}

		@Override
		public String pointerToString() 
		{
			throw new UnsupportedOperationException("Should not be invoked");
		}

		@Override
		public void setPointer(Pointer p) 
		{
			throw new UnsupportedOperationException("Should not be invoked");
		}
			
		public int getDocumentLength()
		{
			return doc_len;
		}

		public void setDocumentLength(int l)
		{
			throw new UnsupportedOperationException("Should not be invoked");
		}
		
		public int getNumberOfEntries() 
		{
			throw new UnsupportedOperationException("Should not be invoked");
		}

		public byte getOffsetBits() 
		{
			throw new UnsupportedOperationException("Should not be invoked");
		}

		public long getOffset() 
		{
			throw new UnsupportedOperationException("Should not be invoked");
		}

		public byte getFileNumber() 
		{
			throw new UnsupportedOperationException("Should not be invoked");
		}

		public void setFileNumber(byte fileId)
		{
			throw new UnsupportedOperationException("Should not be invoked");
		}

		public void setOffset(long _bytes, byte _bits) 
		{
			throw new UnsupportedOperationException("Should not be invoked");
		}

		public String toString()
		{
			return Integer.toString(doc_len);
		}
	}
	
	public static class InputIterator implements Iterator<DocumentIndexEntry>
	{
		final private DocumentIndex doi;
		private int docid;
		
		public InputIterator(final IndexOnDisk index)
		{
			this.doi = index.getDocumentIndex();
			this.docid = 0;
		}
		
		@Override
		public boolean hasNext() 
		{
			return docid < doi.getNumberOfDocuments();
		}

		@Override
		public DocumentIndexEntry next() 
		{
			try {
				return new EFDocumentIndex.Entry(doi.getDocumentLength(docid++));
			} catch (IOException e) {
				throw new IllegalStateException("We should not be here or move beyond the end of the document index iterator");
			}
		}	
	}
}
