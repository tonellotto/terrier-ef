package it.cnr.isti.hpclab.ef;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.FSOMapFileLexiconOutputStream;
import org.terrier.structures.Index;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.LexiconOutputStream;
import org.terrier.structures.collections.FSOrderedMapFile;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.structures.seralization.FixedSizeTextFactory;

import it.cnr.isti.hpclab.ef.structures.EFLexiconEntry;
import it.cnr.isti.hpclab.ef.util.IndexUtil;
import it.cnr.isti.hpclab.ef.util.LongWordBitWriter;
import it.cnr.isti.hpclab.ef.util.SequenceEncoder;

public class BasicCompressor 
{
	protected static final Logger LOGGER = LoggerFactory.getLogger(BasicCompressor.class);

	protected static final int LOG2QUANTUM = Integer.parseInt(System.getProperty(EliasFano.LOG2QUANTUM, "8"));
	protected static final int DEFAULT_CACHE_SIZE = 64 * 1024 * 1024;

	protected final String output_path;
	protected final String output_prefix;
	
	protected final Index src_index;
	protected final int num_docs;
	
	public BasicCompressor(final Index src_index, final String output_path, final String output_prefix)
	{
		this.output_path = output_path;
		this.output_prefix = output_prefix;
		
		if (Index.existsIndex(output_path, output_prefix)) {
			LOGGER.error("Cannot compress index while an index already exists at " + output_path + ", " + output_prefix);
			this.src_index = null;
			this.num_docs = 0;
			return;
		}		
		this.src_index = src_index;		
		this.num_docs = src_index.getCollectionStatistics().getNumberOfDocuments();
	}
	
	@SuppressWarnings("resource")
	public void compress(final int begin_term_id, final int end_term_id) throws IOException
	{
		if (begin_term_id >= end_term_id || begin_term_id < 0 || end_term_id > src_index.getCollectionStatistics().getNumberOfUniqueTerms()) {
			LOGGER.error("Something wrong with termids, begin = " + begin_term_id + ", end = " + end_term_id);
			return;
		}

		// opening src index lexicon iterator and moving to the begin termid
		Iterator<Entry<String, LexiconEntry>> lex_iter = src_index.getLexicon().iterator();
		Entry<String, LexiconEntry> lee = null;
		while (lex_iter.hasNext()) {
			lee = lex_iter.next();
			if (lee.getValue().getTermId() == begin_term_id)
				break;
		}

		// writers
		LexiconOutputStream<String> los    = new FSOMapFileLexiconOutputStream(output_path + File.separator + output_prefix + ".lexicon" + FSOrderedMapFile.USUAL_EXTENSION, new FixedSizeTextFactory(IndexUtil.DEFAULT_MAX_TERM_LENGTH));
		LongWordBitWriter           docids = new LongWordBitWriter(new FileOutputStream(output_path + File.separator + output_prefix + EliasFano.DOCID_EXTENSION).getChannel(), ByteOrder.nativeOrder());
		LongWordBitWriter           freqs  = new LongWordBitWriter(new FileOutputStream(output_path + File.separator + output_prefix + EliasFano.FREQ_EXTENSION).getChannel(), ByteOrder.nativeOrder());
				
		// The sequence encoder to generate posting lists (docids)
		SequenceEncoder docidsAccumulator = new SequenceEncoder( DEFAULT_CACHE_SIZE, LOG2QUANTUM );
		// The sequence encoder to generate posting lists (freqs)
		SequenceEncoder freqsAccumulator = new SequenceEncoder( DEFAULT_CACHE_SIZE, LOG2QUANTUM );
				
		long docidsOffset = 0;
		long freqsOffset = 0;
		
		LexiconEntry le = null;
		IterablePosting p = null;
		
		int local_termid = 0;
		
		while (!stop(lee, end_term_id)) {
			le = lee.getValue();
			p = src_index.getInvertedIndex().getPostings((BitIndexPointer)lee.getValue());
			
			los.writeNextEntry(lee.getKey(), new EFLexiconEntry(local_termid, le.getDocumentFrequency(), le.getFrequency(), docidsOffset, freqsOffset));

			docidsAccumulator.init( le.getDocumentFrequency(), num_docs, false, true, LOG2QUANTUM );
			freqsAccumulator.init(  le.getDocumentFrequency(), le.getFrequency(), true, false, LOG2QUANTUM );
			
			long lastDocid = 0;
			while (p.next() != IterablePosting.END_OF_LIST) {
				docidsAccumulator.add( p.getId() - lastDocid );
				lastDocid = p.getId();
				freqsAccumulator.add(p.getFrequency());
			}
						
			docidsOffset += docidsAccumulator.dump(docids);		
			freqsOffset  += freqsAccumulator.dump(freqs);
			local_termid += 1;
			p.close();
			
			lee = lex_iter.hasNext() ? lex_iter.next() : null;
		} 
				
		docidsAccumulator.close();
		docids.close();
		freqsAccumulator.close();
		freqs.close();
		los.close();
	}
	
	private static boolean stop(final Entry<String, LexiconEntry> lee, final int end)
	{
		return (lee == null || lee.getValue().getTermId() >= end);
	}

}
