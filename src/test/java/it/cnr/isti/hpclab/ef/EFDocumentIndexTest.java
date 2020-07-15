package it.cnr.isti.hpclab.ef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.DocumentIndexEntry;
import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;

public class EFDocumentIndexTest extends EFSetupTest
{
	protected IndexOnDisk originalIndex = null;
	protected IndexOnDisk efIndex = null;

	@Before 
	public void createIndex() throws Exception
	{
		super.doShakespeareIndexing();
		originalIndex = IndexOnDisk.createIndex();
		
		String args[] = {"-path", originalIndex.getPath(), "-prefix", originalIndex.getPrefix() + ".ef", "-index", originalIndex.getPath() + File.separator + originalIndex.getPrefix() + ".properties", "-p", Integer.toString(1)};

		System.setProperty(EliasFano.LOG2QUANTUM, "3");

		Generator.main(args);
		
		efIndex = IndexOnDisk.createIndex(args[1], args[3]);
	}

	@Test
	public void testRandomDocuments() throws IOException
	{
		int nd1 = originalIndex.getCollectionStatistics().getNumberOfDocuments();
		int nd2 = efIndex.getCollectionStatistics().getNumberOfDocuments();
		
		assertEquals(nd1, nd2);
		
		DocumentIndex doi1 = originalIndex.getDocumentIndex();
		DocumentIndex doi2 = efIndex.getDocumentIndex();
		
		assertNotNull(doi1);
		assertNotNull(doi2);
		
		int n = nd1 / 1;
		System.out.println("Testing " + n + " random documents");
		
		Random rnd = new Random();
		while (n-- > 0) {
			int docid = rnd.nextInt(nd1);
			assertEquals(doi1.getDocumentLength(docid), doi2.getDocumentLength(docid));
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testInputStream() throws IOException
	{
		int nd1 = originalIndex.getCollectionStatistics().getNumberOfDocuments();
		int nd2 = efIndex.getCollectionStatistics().getNumberOfDocuments();
		
		assertEquals(nd1, nd2);
		
		Iterator<DocumentIndexEntry> iter1 = (Iterator<DocumentIndexEntry>) originalIndex.getIndexStructureInputStream("document");
		Iterator<DocumentIndexEntry> iter2 = (Iterator<DocumentIndexEntry>) efIndex.getIndexStructureInputStream("document");
		
		assertNotNull(iter1);
		assertNotNull(iter2);
		
		while (iter1.hasNext() && iter2.hasNext()) {
			assertEquals(iter1.next().getDocumentLength(), iter2.next().getDocumentLength());
		}
	
		assertFalse(iter1.hasNext());
		assertFalse(iter2.hasNext());

	}
}
