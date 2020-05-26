package it.cnr.isti.hpclab.ef;

import java.io.IOException;
import java.util.Map.Entry;

import org.terrier.structures.LexiconEntry;

public abstract class Compressor 
{
    protected int cnt = 0;
    
    abstract void compress(final TermPartition terms) throws IOException;
    
    /*
    default boolean stop(final Entry<String, LexiconEntry> lee, final int end)
    {
        return (lee == null || lee.getValue().getTermId() >= end);
    }
    */
    
    final boolean stop(final Entry<String, LexiconEntry> lee, final int len)
    {
        return (lee == null || cnt >= len);
    }

}
