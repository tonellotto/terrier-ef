package it.cnr.isti.hpclab.ef;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import org.terrier.structures.FSOMapFileLexiconOutputStream;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.LexiconOutputStream;
import org.terrier.structures.collections.FSOrderedMapFile;
import org.terrier.structures.seralization.FixedSizeTextFactory;

import it.cnr.isti.hpclab.ef.util.IndexUtil;
import it.cnr.isti.hpclab.ef.util.LongWordBitWriter;
import lombok.Getter;

public class EliasFanoWriters 
{
	protected static final ByteOrder BYTE_ORDER = ByteOrder.nativeOrder();
	
	protected LexiconOutputStream<String> lexOutputStream;

	@Getter protected LongWordBitWriter docidsWriter = null;
	@Getter protected LongWordBitWriter freqsWriter = null;
	
	@Getter protected long docidBitOffset;
	@Getter protected long freqBitOffset;

	@SuppressWarnings("resource")
	public EliasFanoWriters(final String dstIndexPath) throws IOException
	{
		this.lexOutputStream = new FSOMapFileLexiconOutputStream(
				dstIndexPath + ".lexicon" + FSOrderedMapFile.USUAL_EXTENSION, 
				new FixedSizeTextFactory(IndexUtil.DEFAULT_MAX_TERM_LENGTH));
		
		this.docidsWriter = new LongWordBitWriter(
				new FileOutputStream(dstIndexPath + EliasFano.DOCID_EXTENSION).getChannel(), BYTE_ORDER);
		this.freqsWriter = new LongWordBitWriter(
				new FileOutputStream(dstIndexPath + EliasFano.FREQ_EXTENSION).getChannel(),	BYTE_ORDER);
	
		this.docidBitOffset = 0l;
		this.freqBitOffset = 0l;
	}
	
	public void writeLexiconEntry(final String key, final LexiconEntry le) throws IOException
	{
		lexOutputStream.writeNextEntry(key, le);
	}
	
	public void close() throws IOException
	{
		freqsWriter.close();
		docidsWriter.close();
		lexOutputStream.close();
	}
}
