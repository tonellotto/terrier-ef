package it.cnr.isti.hpclab.ef;

import java.io.IOException;

import org.terrier.structures.postings.IterablePosting;

import it.cnr.isti.hpclab.ef.util.SequenceEncoder;
import lombok.Getter;

public class EliasFanoEncoders 
{
	protected static final int DEFAULT_CACHE_SIZE = 64 * 1024 * 1024;
	
	// The sequence encoder to generate posting lists (docids)
	protected SequenceEncoder docidsAccumulator = null;
	// The sequence encoder to generate posting lists (freqs)
	protected SequenceEncoder freqsAccumulator = null;

	protected final int log2quantum;
	
	@Getter protected long lastDumpedDocidBits;
	@Getter protected long lastDumpedFreqBits;
	
	public EliasFanoEncoders(final int log2quantum) throws IOException
	{
		assert log2quantum >= 3;
		
		this.log2quantum = log2quantum;
		// The sequence encoder to generate posting lists (docids)
		this.docidsAccumulator = new SequenceEncoder(DEFAULT_CACHE_SIZE, log2quantum);
		// The sequence encoder to generate posting lists (freqs)
		this.freqsAccumulator = new SequenceEncoder(DEFAULT_CACHE_SIZE, log2quantum);
	}
	
	public void init(final int num, final int docidUb, final int freqUb)
	{
		this.docidsAccumulator.init(num, docidUb, false, true, log2quantum);
		this.freqsAccumulator.init(num, freqUb, true, false, log2quantum);
	}
	
	public void add(final IterablePosting p) throws IOException
	{
        long lastDocid = 0;
        while (p.next() != IterablePosting.END_OF_LIST) {
            docidsAccumulator.add( p.getId() - lastDocid );
            lastDocid = p.getId();
            freqsAccumulator.add(p.getFrequency());
        }
	}

	public void dump(final EliasFanoWriters efWriters) throws IOException
	{
		efWriters.docidBitOffset += docidsAccumulator.dump(efWriters.getDocidsWriter());
		efWriters.freqBitOffset += freqsAccumulator.dump(efWriters.getFreqsWriter());
	}
	
	public void close() throws IOException
	{
		docidsAccumulator.close();
		freqsAccumulator.close();
	}
}
