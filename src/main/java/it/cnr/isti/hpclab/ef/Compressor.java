package it.cnr.isti.hpclab.ef;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.terrier.structures.LexiconEntry;

public interface Compressor 
{
	void compress(final TermPartition terms) throws IOException;
	
	default boolean stop(final Entry<String, LexiconEntry> lee, final int end)
	{
		return (lee == null || lee.getValue().getTermId() >= end);
	}
	
	default Entry<String, LexiconEntry> advanceLexiconIteratorTo(int beginTermId, Iterator<Entry<String, LexiconEntry>> lexIter) 
	{
		Entry<String, LexiconEntry> lee = null;
		while (lexIter.hasNext()) {
			lee = lexIter.next();
			if (lee.getValue().getTermId() == beginTermId)
				break;
		}
		return lee;
	}
}
