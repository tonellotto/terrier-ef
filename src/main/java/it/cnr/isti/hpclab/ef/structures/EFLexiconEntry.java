package it.cnr.isti.hpclab.ef.structures;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.terrier.structures.BitFilePosition;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.EntryStatistics;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.seralization.FixedSizeWriteableFactory;

/** 
 * Contains all the information about one entry in the Lexicon.
 * Based on the BasicLexiconEntry class in Terrier. 
 * Include offsets for docid and freq files compressed with Elias-Fano.  
 */

public class EFLexiconEntry extends LexiconEntry implements BitIndexPointer
{
	private static final long serialVersionUID = 1L;

	/** the term id of this entry */
	public int termId;
	/** the number of document that this entry occurs in */
	public int n_t;
	/** the total number of occurrences of the term in the index */
	public int TF;
	/** the largest in-document term frequency in the documents that this entry occurs in */
	public int maxtf;
	
	/** the offsets we need */
	public long docidOffset;
	public long freqOffset;
	
	/** 
	 * Factory for creating EFLexiconEntry objects
	 */
	public static class Factory implements FixedSizeWriteableFactory<LexiconEntry>
	{
		/** {@inheritDoc} */
		@Override
		public int getSize() 
		{
			return 4 * Integer.BYTES + 2 * Long.BYTES;
		}
		
		/** {@inheritDoc} */
		@Override
		public LexiconEntry newInstance() 
		{
			return new EFLexiconEntry();
		}
	}
	
	/** 
	 * Create an empty EFLexiconEntry.
	 */
	public EFLexiconEntry() 
	{
	}
	
	/** 
	 * Create a lexicon entry with the following information:
	 * 
	 * @param tid the term id
	 * @param n_t the number of documents the term occurs in (document frequency)
	 * @param TF the total count of term t in the collection
	 * @param docidOffset the bit offset of the posting list in the docid file
	 * @param freqOffset the bit offset of the posting list in the freq file
	 */
	public EFLexiconEntry(int tid, int n_t, int TF, long docidOffset, long freqOffset) 
	{
		this(tid, n_t, TF, Integer.MAX_VALUE, docidOffset, freqOffset);		
	}
	
	/** 
	 * Create a lexicon entry with the following information:
	 * 
	 * @param tid the term id
	 * @param n_t the number of documents the term occurs in (document frequency)
	 * @param TF the total count of term t in the collection
	 * @param maxtf the largest in-document term frequency in the posting list
	 * @param docidOffset the bit offset of the posting list in the docid file
	 * @param freqOffset the bit offset of the posting list in the freq file
	 */
	public EFLexiconEntry(int tid, int n_t, int TF, int maxtf, long docidOffset, long freqOffset) 
	{
		this.termId = tid;
		this.n_t = n_t;
		this.TF = TF;
		
		this.docidOffset = docidOffset;
		this.freqOffset = freqOffset;
		
		this.maxtf = maxtf;
	}

	/** 
	 * Return the bit offset of the posting list in the docid file 
	 */
	public long getDocidOffset() 
	{
		return this.docidOffset;
	}

	/** 
	 * Return the bit offset of the posting list in the freq file 
	 */
	public long getFreqOffset() 
	{
		return this.freqOffset;
	}

	/** {@inheritDoc} */
	@Override
	public void readFields(DataInput in) throws IOException 
	{
		this.termId      = in.readInt();
		this.TF          = in.readInt();
		this.n_t         = in.readInt();
		this.maxtf		 = in.readInt();
		this.docidOffset = in.readLong();
		this.freqOffset  = in.readLong();
	}
	
	/** {@inheritDoc} */
	@Override
	public void write(DataOutput out) throws IOException 
	{
		out.writeInt (termId);
		out.writeInt (TF);
		out.writeInt (n_t);
		out.writeInt (maxtf);
		out.writeLong(docidOffset);
		out.writeLong(freqOffset);
	}

	/** {@inheritDoc} */
	@Override
	public String toString()
	{
		return "term "+ termId + " Nt = " + n_t + " TF = " + TF  + "max tf = " + maxtf + " [docid @ " + this.docidOffset + " freq @ " + this.freqOffset + "]";
	}

	/** {@inheritDoc} */
	@Override
	public int getFrequency() 
	{
		return TF;
	}

	/** {@inheritDoc} */
	@Override
	public int getDocumentFrequency() 
	{
		return n_t;
	}

	/** {@inheritDoc} */
	@Override
	public int getTermId() 
	{
		return termId;
	}

	/** {@inheritDoc} */
	@Override
	public void setTermId(int newTermId) 
	{
		this.termId = newTermId;
	}

	/** {@inheritDoc} */
	@Override
	public void setStatistics(int n_t, int TF) 
	{
		this.n_t = n_t;
		this.TF = TF;
	}
	
	/** {@inheritDoc} */
	@Override
	public void setNumberOfEntries(int n) 
	{
		this.n_t = n;
	}
	
	/** {@inheritDoc} */
	@Override 
	public int getNumberOfEntries() 
	{
		return this.n_t;
	}
	
	/** {@inheritDoc} */
	@Override 
	public void add(EntryStatistics e) 					  
	{
		this.n_t += e.getDocumentFrequency();
		this.TF  += e.getFrequency();
		if (e.getMaxFrequencyInDocuments() > maxtf)
			maxtf = e.getMaxFrequencyInDocuments();
	}
	
	/** {@inheritDoc} */
	@Override 
	public void subtract(EntryStatistics e)
	{ 
		this.n_t -= e.getDocumentFrequency();
		this.TF  -= e.getFrequency();
	}
	
	// Bunch of unnecessary methods related to the original Terrier inverted file format
	@Override public long getOffset() 								  { throw new RuntimeException("Should not be invoked"); }
	@Override public byte getOffsetBits() 							  { throw new RuntimeException("Should not be invoked"); }
	@Override public void setOffset(long bytes, byte bits) 			  { throw new RuntimeException("Should not be invoked"); }
	@Override public void setOffset(BitFilePosition pos) 			  { throw new RuntimeException("Should not be invoked"); }
	@Override public void setBitIndexPointer(BitIndexPointer pointer) { throw new RuntimeException("Should not be invoked"); }
	@Override public void setFileNumber(byte fileId) 				  { throw new RuntimeException("Should not be invoked"); }
	@Override public byte getFileNumber() 							  { throw new RuntimeException("Should not be invoked"); }

	/** {@inheritDoc} */
	@Override
	public int getMaxFrequencyInDocuments() 
	{
		return this.maxtf;
	}

	/** {@inheritDoc} */
	@Override
	public void setMaxFrequencyInDocuments(int max) 
	{
		this.maxtf = max;
	}
}
