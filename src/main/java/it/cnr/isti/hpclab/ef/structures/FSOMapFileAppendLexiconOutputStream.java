package it.cnr.isti.hpclab.ef.structures;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import org.terrier.structures.LexiconEntry;
import org.terrier.structures.LexiconOutputStream;

import org.terrier.structures.collections.FSOrderedMapFile;
import org.terrier.structures.collections.FSOrderedMapFile.MapFileWriter;

import org.terrier.structures.seralization.FixedSizeWriteableFactory;

/**
 * This class is a copy of the org.terrier.structures.FSOMapFileLexiconOutputStream,
 * but the only difference being the underlying map file (local filesystem only), which is opened in append mode.
 * This class is used only by the CompressorReducer class, to avoid wasting time in copying lexicons. 
 */
public class FSOMapFileAppendLexiconOutputStream extends LexiconOutputStream<String>
{
	private final int key_size, value_size;
	private final Text tempKey;
	private final FSOrderedMapFile.MapFileWriter mapFileWriter;

	public FSOMapFileAppendLexiconOutputStream(final String filename, final FixedSizeWriteableFactory<Text> keyFactory, final FixedSizeWriteableFactory<LexiconEntry> valueFactory) throws IOException 
	{	
		this.mapFileWriter = mapFileWrite(filename);
		this.tempKey = keyFactory.newInstance();
		this.key_size   = keyFactory.getSize();
		this.value_size = valueFactory.getSize();
	}

	@Override
	public int writeNextEntry(String _key, LexiconEntry _value) throws IOException 
	{
		setKey(_key);
		mapFileWriter.write(tempKey, _value);
		super.incrementCounters(_value);
		return key_size + value_size;
	}
	
	@Override
	public void close() 
	{
		super.close();
		try {
			mapFileWriter.close();
		} catch (Exception ioe) {
			ioe.printStackTrace();
		}
	}
			
	protected void setKey(String k) 
	{
		tempKey.set(k);
	}	
	
    /* 
     * Returns a utility class which can be used to write a FSOrderedMapFile. 
     * Input data MUST be sorted by key. 
     */
    public static MapFileWriter mapFileWrite(final String filename) throws IOException
    {
        return new MapFileWriter() 
        {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename, true)));
            
            @SuppressWarnings("rawtypes")
			public void write(WritableComparable key, Writable value) throws IOException
            {
                key.write(out);
                value.write(out);
            }
            
            @Override
            public void close() throws IOException
            {
                out.close();
            }
        };
    }
    
    public int getEntrySize()
    {
    	return key_size + value_size; 
    }
}