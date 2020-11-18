package it.cnr.isti.hpclab.ef;

import java.io.FileOutputStream;
import java.io.IOException;

import org.terrier.structures.LexiconEntry;

import it.cnr.isti.hpclab.ef.util.LongWordBitWriter;
import lombok.Getter;

public class EliasFanoBlockWriters extends EliasFanoWriters
{
	@Getter protected LongWordBitWriter posWriter = null;
	
	protected long posBitOffset;

	@SuppressWarnings("resource")
	public EliasFanoBlockWriters(final String dstIndexPath) throws IOException
	{
		super(dstIndexPath);
		
        this.posWriter = new LongWordBitWriter(
	        new FileOutputStream(dstIndexPath + EliasFano.POS_EXTENSION).getChannel(), BYTE_ORDER);		
		this.posBitOffset = 0l;
	}
	
	@Override
	public void writeLexiconEntry(final String key, final LexiconEntry le) throws IOException
	{
		lexOutputStream.writeNextEntry(key, le);
	}
	
	@Override
	public void close() throws IOException
	{
		super.close();
		posWriter.close();
	}
}
