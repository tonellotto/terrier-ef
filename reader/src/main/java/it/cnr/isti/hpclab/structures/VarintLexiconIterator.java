package it.cnr.isti.hpclab.structures;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.seralization.WriteableFactory;

import it.unimi.dsi.io.ByteBufferInputStream;

public class VarintLexiconIterator implements Iterator<Entry<String, LexiconEntry>>, Closeable 
{	
	int idx;

	private ByteBufferInputStream offsetsFile;
	private DataInputStream offsetsDis;
	
	private ByteBufferInputStream dataFile;
	private DataInputStream dataDis;
	
	private WriteableFactory<LexiconEntry> factory;

	private int numberOfEntries;

	@SuppressWarnings("unchecked")
	public VarintLexiconIterator(IndexOnDisk index) throws IOException 
	{
		this.idx = 0;
		
		String subpath = index.getPath() + File.separator + index.getPrefix();
		
		String offsetspath = subpath + ".lexicon-offsets";		
		File of = new File(offsetspath);
		offsetsFile = ByteBufferInputStream.map(FileChannel.open(of.toPath(), StandardOpenOption.READ));
		offsetsDis = new DataInputStream(offsetsFile);
		
		numberOfEntries = (int) (of.length() / Long.BYTES);		
		
		String datapath = subpath + ".lexicon-data";		
		dataFile = ByteBufferInputStream.map(FileChannel.open((new File(datapath)).toPath(), StandardOpenOption.READ));
		this.dataDis = new DataInputStream(dataFile);
		
		this.factory = (WriteableFactory<LexiconEntry>)index.getIndexStructure("lexicon-valuefactory");	
	}

	public VarintLexiconIterator(int numberOfEntries, ByteBufferInputStream offsetsFile, ByteBufferInputStream dataFile, WriteableFactory<LexiconEntry> factory) 
	{
		this.idx = 0;
		
		this.numberOfEntries = numberOfEntries;
		
		this.offsetsFile = offsetsFile;
		this.offsetsDis = new DataInputStream(offsetsFile);
			
		this.dataFile = dataFile;
		this.dataDis = new DataInputStream(dataFile);
		
		this.factory = factory;	
	}

	@Override
	public boolean hasNext() 
	{	
		return idx < numberOfEntries;
	}

	@Override
	public Entry<String, LexiconEntry> next() 
	{	
		if (!hasNext()) {
			return null;
		} else {
			String key = null;			
			LexiconEntry le = factory.newInstance();
			le.setTermId(idx);
			
			try {		
				offsetsFile.position(((long)idx) * Long.BYTES);
				long offset = offsetsDis.readLong();
				
				dataFile.position(offset);
				
				key = dataDis.readUTF();
				
				le.readFields(dataDis);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			idx++;
			
			return new AbstractMap.SimpleEntry<String, LexiconEntry>(key, le);
		}
	}

	@Override
	public void close() throws IOException 
	{
		dataDis.close();
		offsetsDis.close();
	}
}