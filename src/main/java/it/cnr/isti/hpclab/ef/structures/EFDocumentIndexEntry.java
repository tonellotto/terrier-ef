package it.cnr.isti.hpclab.ef.structures;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.terrier.structures.BitFilePosition;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.DocumentIndexEntry;
import org.terrier.structures.Pointer;

/**
 * This class is a simple wrapper to allow EFDirectIndex to 
 * correctly implement the BitPostingIndex interface.
 */
public class EFDocumentIndexEntry extends DocumentIndexEntry
{
	protected final int docid;
	
	public EFDocumentIndexEntry(int docid) 
	{
		this.docid = docid;
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

}
