package it.cnr.isti.hpclab.structures;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.terrier.structures.LexiconEntry;

import it.cnr.isti.hpclab.util.Varint;

import org.terrier.structures.BitIndexPointer;

import org.terrier.structures.seralization.WriteableFactory;

import it.cnr.isti.hpclab.ef.structures.EFBlockLexiconEntry;

public class VarintBlockLexiconEntry extends EFBlockLexiconEntry implements BitIndexPointer
{
	private static final long serialVersionUID = 1L;
	
	public static class Factory implements WriteableFactory<LexiconEntry>
	{
		@Override
		public LexiconEntry newInstance() 
		{
			return new VarintBlockLexiconEntry();
		}
	}
	
	public VarintBlockLexiconEntry() 
	{
	}
	
	public VarintBlockLexiconEntry(int tid, int n_t, int TF, long docidOffset, long freqOffset, long posOffset, long sumsMaxPos)  
	{
		super(tid, n_t, TF, docidOffset, freqOffset, posOffset, sumsMaxPos);
	}
			
	@Override
	public void readFields(DataInput in) throws IOException 
	{
		this.TF          = Varint.readUnsignedInt(in);
		this.n_t         = Varint.readUnsignedInt(in);
		this.docidOffset = Varint.readUnsignedLong(in);
		this.freqOffset  = Varint.readUnsignedLong(in);
		this.posOffset   = Varint.readUnsignedLong(in);
		this.sumsMaxPos  = Varint.readUnsignedLong(in);
	}
	
	@Override
	public void write(DataOutput out) throws IOException 
	{
		Varint.writeUnsignedInt(TF, out);
		Varint.writeUnsignedInt(n_t, out);
		Varint.writeUnsignedLong(docidOffset, out);
		Varint.writeUnsignedLong(freqOffset, out);
		Varint.writeUnsignedLong(posOffset, out);
		Varint.writeUnsignedLong(sumsMaxPos, out);
	}

	@Override
	public String toString()
	{
		return "term "+ termId + " Nt = " + n_t + " TF = " + TF  + " [docid @ " + this.docidOffset + " freq @ " + this.freqOffset + "]" + " [sumsMaxPos = " +  this.sumsMaxPos + " pos @ " + this.posOffset + "]";
	}
}