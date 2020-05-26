package it.cnr.isti.hpclab.ef;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.bit.BitPostingIndexInputStream;
import org.terrier.structures.postings.IterablePosting;

import it.cnr.isti.hpclab.ef.structures.EFInvertedIndex;

public class EFInvertedIndexIteratorTest extends EFSetupTest
{
	protected IndexOnDisk originalIndex = null;
	protected IndexOnDisk efIndex = null;

	@Before 
	public void createIndex() throws Exception
	{
		super.doShakespeareIndexing();
		originalIndex = Index.createIndex();
		
		String args[] = {"-path", originalIndex.getPath(), "-prefix", originalIndex.getPrefix() + ".ef", "-index", originalIndex.getPath() + File.separator + originalIndex.getPrefix() + ".properties", "-p", Integer.toString(1)};

		System.setProperty(EliasFano.LOG2QUANTUM, "3");

		Generator.main(args);
		
		efIndex = Index.createIndex(args[1], args[3]);
	}
	
	@Test
	public void testInputStream() throws IOException
	{

		BitPostingIndexInputStream iter1 = (BitPostingIndexInputStream) originalIndex.getIndexStructureInputStream("inverted");
		EFInvertedIndex.InputIterator iter2 = (EFInvertedIndex.InputIterator) efIndex.getIndexStructureInputStream("inverted");
		
		assertNotNull(iter1);
		assertNotNull(iter2);
		
		IterablePosting p1;
		IterablePosting p2;
		while (iter1.hasNext() && iter2.hasNext()) {
			p1 = iter1.next();
			p2 = iter2.next();
			assertEquals(p1, p2);
		}
	
		assertFalse(iter1.hasNext());
		assertFalse(iter2.hasNext());

	}
	
    static public void assertEquals(IterablePosting expected, IterablePosting actual) 
    {
    	try {
    		
			while (expected.next() != IterablePosting.EOL && actual.next() != IterablePosting.EOL) {
				Assert.assertEquals(expected.getId(), actual.getId());
				Assert.assertEquals(expected.getFrequency(), actual.getFrequency());
				Assert.assertEquals(expected.getDocumentLength(), actual.getDocumentLength());
			}
			
			assertTrue(expected.next() == IterablePosting.EOL);
			assertTrue(actual.next() == IterablePosting.EOL);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}