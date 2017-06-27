package it.cnr.isti.hpclab.structures;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.terrier.structures.LexiconEntry;
import org.terrier.structures.LexiconOutputStream;

public class VarintLexiconOutputStream extends LexiconOutputStream<String> 
{	
	private LargeDataOutputStream dosData;
	private LargeDataOutputStream dosOffsets;
	
	private String prevKey;
		
	public VarintLexiconOutputStream(final String path, final String prefix) throws FileNotFoundException 
	{
		dosData    = new LargeDataOutputStream(new BufferedOutputStream(new FileOutputStream(path + File.separator + prefix + ".lexicon" + VarintLexicon.LEXICON_DATA   )));
		dosOffsets = new LargeDataOutputStream(new BufferedOutputStream(new FileOutputStream(path + File.separator + prefix + ".lexicon" + VarintLexicon.LEXICON_OFFSETS)));
		
		this.prevKey = null;
	}
	
	@Override
	public int writeNextEntry(final String _key, final LexiconEntry _value) throws IOException 
	{
		if (prevKey != null) {
			if (prevKey.compareTo(_key) > 0) {
				throw new IllegalArgumentException("Lexicon entries must be lexicographically sorted!");
			}			
		}
		
		long curOffset = dosData.size();
		dosOffsets.writeLong(curOffset);
		
		dosData.writeUTF(_key);
		_value.write(dosData);
		
		prevKey = _key;
		
		return (int) (dosData.size() - curOffset);
	}
	
	public void close() 
	{	
		try {		
			dosOffsets.close();
			dosData.close();					
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}