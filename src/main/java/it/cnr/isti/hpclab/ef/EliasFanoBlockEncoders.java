package it.cnr.isti.hpclab.ef;

import java.io.IOException;

import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.IterablePosting;

import it.cnr.isti.hpclab.ef.util.SequenceEncoder;

public class EliasFanoBlockEncoders extends EliasFanoEncoders 
{
    // The sequence encoder to generate posting lists (positions)
    protected SequenceEncoder posAccumulator = null;

	// for posAccumulator initialization
	protected long sumMaxPos = 0; // in the first pass, we need to compute the upper bound to encode positions
	protected int freqUb = 0;
	protected int num = 0;
	

	public EliasFanoBlockEncoders(final int log2quantum) throws IOException
	{
		super(log2quantum);

		// The sequence encoder to generate posting lists (positions)
		posAccumulator = new SequenceEncoder(DEFAULT_CACHE_SIZE, log2quantum);
	}
	
	@Override
	public void init(final int num, final int docidUb, final int freqUb)
	{	
		this.docidsAccumulator.init(num, docidUb, false, true, log2quantum);
		this.freqsAccumulator.init(num, freqUb, true, false, log2quantum);
		
		this.num = num;
		this.freqUb = freqUb;
	}
	
	@Override
	public void add(final IterablePosting p) throws IOException
	{
        long posOccurrencies = 0; // Do not trust le.getFrequency() because of block max limit!
        long lexOccurrencies = 0;
        
        long lastDocid = 0;
        while (p.next() != IterablePosting.END_OF_LIST) {
        	docidsAccumulator.add( p.getId() - lastDocid );
        	lastDocid = p.getId();
        	freqsAccumulator.add(p.getFrequency());

        	sumMaxPos += ((BlockPosting)p).getPositions()[((BlockPosting)p).getPositions().length - 1];
        	lexOccurrencies += p.getFrequency();
        	posOccurrencies += ((BlockPosting)p).getPositions().length;
		}
        
        if (posOccurrencies != lexOccurrencies)
            throw new IllegalStateException("Lexicon term occurencies (" + lexOccurrencies + ") different form positions-counted occurrencies (" + posOccurrencies + ")");
	}

	public void addPositions(final IterablePosting p) throws IOException
	{
		assert sumMaxPos > 0 && num > 0 && freqUb > 0;
		
		this.posAccumulator.init(freqUb, num + sumMaxPos, true, false, log2quantum );
		
        int[] positions = null;
        while (p.next() != IterablePosting.END_OF_LIST) {
            positions = ((BlockPosting)p).getPositions();
            posAccumulator.add(1 + positions[0]);
            for (int i = 1; i < positions.length; i++)
                posAccumulator.add(positions[i] - positions[i-1]);
        }
	}

	@Override
	public void dump(final EliasFanoWriters efWriters) throws IOException
	{
		EliasFanoBlockWriters befWriters = (EliasFanoBlockWriters)efWriters;
		
		befWriters.docidBitOffset += docidsAccumulator.dump(befWriters.getDocidsWriter());
		befWriters.freqBitOffset += freqsAccumulator.dump(befWriters.getFreqsWriter());
		
        // Firstly we write decoding limits info
		befWriters.posBitOffset += befWriters.getPosWriter().writeGamma(posAccumulator.lowerBits());
		befWriters.posBitOffset += posAccumulator.numberOfPointers() == 0 ? 0 : befWriters.getPosWriter().writeNonZeroGamma(posAccumulator.pointerSize());
        // Secondly we dump the EF representation of the position encoding
		befWriters.posBitOffset += posAccumulator.dump(befWriters.getPosWriter());
	}
	
	@Override
	public void close() throws IOException
	{
		docidsAccumulator.close();
		freqsAccumulator.close();
	}
}
