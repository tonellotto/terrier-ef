package it.cnr.isti.hpclab.ef.structures;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.terrier.structures.LexiconEntry;

public class EFPosLexiconEntry extends EFLexiconEntry
{
	private static final long serialVersionUID = 1L;
	
	/** the offsets we need */
	private long posOffset;

	private long sumsMaxPos = 0l;
	
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
			return new EFPosLexiconEntry();
		}
	}
	
	public EFPosLexiconEntry() 
	{
	}

	public EFPosLexiconEntry(int tid, int n_t, int TF, long docidOffset, long freqOffset, long posOffset, long sumsMaxPos) 
	{
		super(tid, n_t, TF, docidOffset, freqOffset);
		this.posOffset = posOffset;
		this.sumsMaxPos = sumsMaxPos;
	}

	@Override
	public void readFields(DataInput in) throws IOException 
	{
		super.readFields(in);
		this.posOffset = in.readLong();
		this.sumsMaxPos = in.readLong();
	}
	
	@Override
	public void write(DataOutput out) throws IOException 
	{
		super.write(out);
		out.writeLong(this.posOffset);
		out.writeLong(this.sumsMaxPos);
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
}
