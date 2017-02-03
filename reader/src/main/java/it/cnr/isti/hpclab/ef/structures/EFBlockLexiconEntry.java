package it.cnr.isti.hpclab.ef.structures;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.terrier.structures.LexiconEntry;

public class EFBlockLexiconEntry extends EFLexiconEntry
{
	private static final long serialVersionUID = 1L;
	
	/** the offsets we need */
	private long posOffset;

	private long sumsMaxPos = 0l;
	// private long occurrencies = 0l;
	
	public static class Factory extends EFLexiconEntry.Factory
	{
		@Override
		public int getSize() 
		{
			return super.getSize() + 2 * Long.BYTES;
		}
		
		@Override
		public LexiconEntry newInstance() 
		{
			return new EFBlockLexiconEntry();
		}
	}
	
	public EFBlockLexiconEntry() 
	{
	}

	public EFBlockLexiconEntry(int tid, int n_t, int TF, long docidOffset, long freqOffset, long posOffset, long sumsMaxPos/*, long occurrencies*/) 
	{
		super(tid, n_t, TF, docidOffset, freqOffset);
		this.posOffset = posOffset;
		this.sumsMaxPos = sumsMaxPos;
		// this.occurrencies = occurrencies;
	}

	@Override
	public void readFields(DataInput in) throws IOException 
	{
		super.readFields(in);
		this.posOffset = in.readLong();
		this.sumsMaxPos = in.readLong();
		// this.occurrencies = in.readLong();
	}
	
	@Override
	public void write(DataOutput out) throws IOException 
	{
		super.write(out);
		out.writeLong(this.posOffset);
		out.writeLong(this.sumsMaxPos);
		// out.writeLong(this.occurrencies);
	}

	@Override
	public String toString()
	{
		return super.toString() + " [sumsMaxPos = " +  this.sumsMaxPos + " pos @ " + this.posOffset + "]";
	}
	
	public long getPosOffset() 
	{
		return this.posOffset;
	}
	
	public long getSumsMaxPos()
	{
		return this.sumsMaxPos;
	}
	
	/*
	public long getOccurrencies()
	{
		return this.occurrencies;
	}
	*/

}
