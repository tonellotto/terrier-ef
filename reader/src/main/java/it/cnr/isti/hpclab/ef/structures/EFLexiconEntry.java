package it.cnr.isti.hpclab.ef.structures;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.terrier.structures.BitFilePosition;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.EntryStatistics;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.seralization.FixedSizeWriteableFactory;

public class EFLexiconEntry extends LexiconEntry implements BitIndexPointer
{
	private static final long serialVersionUID = 1L;

	/** the term id of this entry */
	public int termId;
	/** the number of document that this entry occurs in */
	public int n_t;
	/** the total number of occurrences of the term in the index */
	public int TF;

	/** the offsets we need */
	private long docidOffset;
	private long freqOffset;
	
	public static class Factory implements FixedSizeWriteableFactory<LexiconEntry>
	{
		@Override
		public int getSize() 
		{
			return 3 * Integer.BYTES + 2 * Long.BYTES;
		}
		
		@Override
		public LexiconEntry newInstance() 
		{
			return new EFLexiconEntry();
		}
	}
	
	public EFLexiconEntry() 
	{
	}
	
	public EFLexiconEntry(int tid, int n_t, int TF, long docidOffset, long freqOffset) 
	{
		this.termId = tid;
		this.n_t = n_t;
		this.TF = TF;
		
		this.docidOffset = docidOffset;
		this.freqOffset = freqOffset;
	}
	
	public long getDocidOffset() 
	{
		return this.docidOffset;
	}

	public long getFreqOffset() 
	{
		return this.freqOffset;
	}

	@Override
	public void readFields(DataInput in) throws IOException 
	{
		this.termId      = in.readInt();
		this.TF          = in.readInt();
		this.n_t         = in.readInt();
		this.docidOffset = in.readLong();
		this.freqOffset  = in.readLong();
	}
	
	@Override
	public void write(DataOutput out) throws IOException 
	{
		out.writeInt (termId);
		out.writeInt (TF);
		out.writeInt (n_t);
		out.writeLong(docidOffset);
		out.writeLong(freqOffset);
	}

	@Override
	public String toString()
	{
		return "term "+ termId + " Nt = " + n_t + " TF = " + TF  + " [docid @ " + this.docidOffset + " freq @ " + this.freqOffset + "]";
	}

	@Override
	public int getFrequency() 
	{
		return TF;
	}

	@Override
	public int getDocumentFrequency() 
	{
		return n_t;
	}

	@Override
	public int getTermId() 
	{
		return termId;
	}

	@Override
	public void setTermId(int newTermId) 
	{
		this.termId = newTermId;
	}

	@Override
	public void setStatistics(int n_t, int TF) 
	{
		this.n_t = n_t;
		this.TF = TF;
	}
	
	@Override
	public void setNumberOfEntries(int n) 
	{
		this.n_t = n;
	}
	
	@Override 
	public int getNumberOfEntries() 
	{
		return this.n_t;
	}

	// Bunch of unnecessary methods I do not want to implement :-)
	
	@Override public void add(EntryStatistics e) 					  { throw new RuntimeException("Should not be invoked"); }
	@Override public void subtract(EntryStatistics e) 				  { throw new RuntimeException("Should not be invoked"); }
	@Override public long getOffset() 								  { throw new RuntimeException("Should not be invoked"); }
	@Override public byte getOffsetBits() 							  { throw new RuntimeException("Should not be invoked"); }
	@Override public void setOffset(long bytes, byte bits) 			  { throw new RuntimeException("Should not be invoked"); }
	@Override public void setOffset(BitFilePosition pos) 			  { throw new RuntimeException("Should not be invoked"); }
	@Override public void setBitIndexPointer(BitIndexPointer pointer) { throw new RuntimeException("Should not be invoked"); }
	@Override public void setFileNumber(byte fileId) 				  { throw new RuntimeException("Should not be invoked"); }
	@Override public byte getFileNumber() 							  { throw new RuntimeException("Should not be invoked"); }
}
