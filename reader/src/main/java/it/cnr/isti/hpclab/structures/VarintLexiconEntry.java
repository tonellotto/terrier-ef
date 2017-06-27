package it.cnr.isti.hpclab.structures;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.LexiconEntry;

import org.terrier.structures.seralization.WriteableFactory;

import it.cnr.isti.hpclab.ef.structures.EFLexiconEntry;
import it.cnr.isti.hpclab.util.Varint;

public class VarintLexiconEntry extends EFLexiconEntry implements BitIndexPointer
{
	private static final long serialVersionUID = 1L;

	public static class Factory implements WriteableFactory<LexiconEntry>
	{
		@Override
		public LexiconEntry newInstance() 
		{
			return new VarintLexiconEntry();
		}
	}
	
	public VarintLexiconEntry() 
	{
	}
	
	public VarintLexiconEntry(int tid, int n_t, int TF, long docidOffset, long freqOffset) 
	{
		super(tid, n_t, TF, docidOffset, freqOffset);
	}
	
	@Override
	public void readFields(DataInput in) throws IOException 
	{
		super.TF          = Varint.readUnsignedInt(in);
		super.n_t         = Varint.readUnsignedInt(in);
		super.docidOffset = Varint.readUnsignedLong(in);
		super.freqOffset  = Varint.readUnsignedLong(in);
	}
	
	@Override
	public void write(DataOutput out) throws IOException 
	{
		Varint.writeUnsignedInt(TF, out);
		Varint.writeUnsignedInt(n_t, out);
		Varint.writeUnsignedLong(docidOffset, out);
		Varint.writeUnsignedLong(freqOffset, out);
	}

	@Override
	public String toString()
	{
		return "term "+ termId + " Nt = " + n_t + " TF = " + TF  + " [docid @ " + super.docidOffset + " freq @ " + super.freqOffset + "]";
	}
}