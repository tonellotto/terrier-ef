package it.cnr.isti.hpclab.structures;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.seralization.WriteableFactory;

import it.unimi.dsi.io.ByteBufferInputStream;

public class VarintLexicon extends Lexicon<String> 
{	
	// public static final String USUAL_EXTENSION = ".varint";
	
	public static final String LEXICON_DATA    = ".lex-data";
	public static final String LEXICON_OFFSETS = ".lex-offsets";
	
	protected int numberOfEntries;
	
	protected ByteBufferInputStream offsetsFile;
	private DataInputStream offsetsDis;
	
	protected ByteBufferInputStream dataFile; 
	private DataInputStream dataDis;
	
	private  WriteableFactory<LexiconEntry> factory;
	
	@SuppressWarnings("unchecked")
	public VarintLexicon(final IndexOnDisk index) throws IOException 
	{	
		String path_prefix = index.getPath() + File.separator + index.getPrefix();
		
		String offsets_path = path_prefix + ".lexicon" + LEXICON_OFFSETS;		
		offsetsFile = ByteBufferInputStream.map(FileChannel.open((new File(offsets_path)).toPath(), StandardOpenOption.READ));
		offsetsDis = new DataInputStream(offsetsFile);
				
		String data_path = path_prefix + ".lexicon" + LEXICON_DATA;		
		dataFile = ByteBufferInputStream.map(FileChannel.open((new File(data_path)).toPath(), StandardOpenOption.READ));
		dataDis = new DataInputStream(dataFile);
		
		factory = (WriteableFactory<LexiconEntry>)index.getIndexStructure("lexicon-valuefactory");
		numberOfEntries = index.getCollectionStatistics().getNumberOfUniqueTerms();
	}
	
	@Override
	public void close() throws IOException 
	{
		offsetsDis.close();
		dataDis.close();
	}

	@Override
	public Iterator<Entry<String, LexiconEntry>> iterator() 
	{	
		return new VarintLexiconIterator(numberOfEntries, offsetsFile.copy(), dataFile.copy(), factory);
	}

	@Override
	public int numberOfEntries() 
	{	
		return numberOfEntries;
	}

	protected int binarySearch(final String key) throws IOException 
	{
        int lo = 0;
        int hi = numberOfEntries - 1;
        String midKey;
        
        while (lo <= hi) {        	
			try {
				int mid = lo + (hi - lo) / 2;
				offsetsFile.position((((long)mid)) * Long.BYTES);
				long midOffset = offsetsDis.readLong();
				dataFile.position(midOffset);
				midKey = dataDis.readUTF();
				if (key.compareTo(midKey) < 0) {
					hi = mid - 1;
					continue;
				} else if (key.compareTo(midKey) > 0) {
					lo = mid + 1;
					continue;
				} else {
					return mid;
				}
			} catch (IOException e) {
				throw e;
			}
        }
        return -1;
	}
	
	@Override
	public LexiconEntry getLexiconEntry(final String key) 
	{
		try {
			int idx = binarySearch(key);
			if (idx > -1) {
	        	try {
	        		Entry<String, LexiconEntry> ithLexiconEntry = getIthLexiconEntry(idx);
	        		return ithLexiconEntry.getValue();
	        	} catch (NoSuchElementException e) {
	        		return null;
	        	}
			} else {
				return null;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
	}

	@Override
	public Entry<String, LexiconEntry> getLexiconEntry(final int termid) 
	{
		try {
			return getIthLexiconEntry(termid);
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	@Override
	public Entry<String, LexiconEntry> getIthLexiconEntry(final int index) 
	{
		if (index < 0 || index >= numberOfEntries) throw new NoSuchElementException();
		
		try {		
			offsetsFile.position(((long)index) * Long.BYTES);
			long offset = offsetsDis.readLong();
						
			dataFile.position(offset);
			
			String key = dataDis.readUTF();
			
			LexiconEntry le = factory.newInstance();
			le.setTermId(index);
			le.readFields(dataDis);
			
			return new AbstractMap.SimpleEntry<String, LexiconEntry>(key, le);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	@Override
	public Iterator<Entry<String, LexiconEntry>> getLexiconEntryRange(String from, String to) 
	{ 
		throw new UnsupportedOperationException(); 
	}
}