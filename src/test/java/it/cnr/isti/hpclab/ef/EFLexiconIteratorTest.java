package it.cnr.isti.hpclab.ef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.terrier.structures.FSOMapFileLexicon.MapFileLexiconIterator;
import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.LexiconEntry;

public class EFLexiconIteratorTest extends EFSetupTest
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
		int nt1 = originalIndex.getCollectionStatistics().getNumberOfUniqueTerms();
		int nt2 = efIndex.getCollectionStatistics().getNumberOfUniqueTerms();
		
		assertEquals(nt1, nt2);

		MapFileLexiconIterator iter1 = (MapFileLexiconIterator) originalIndex.getIndexStructureInputStream("lexicon");
		MapFileLexiconIterator iter2 = (MapFileLexiconIterator) efIndex.getIndexStructureInputStream("lexicon");
		
		assertNotNull(iter1);
		assertNotNull(iter2);
		
		Map.Entry<String, LexiconEntry> e1;
		Map.Entry<String, LexiconEntry> e2;
		while (iter1.hasNext() && iter2.hasNext()) {
			e1 = iter1.next();
			e2 = iter2.next();
			assertEquals(e1.getKey(), e2.getKey());
			assertEquals(e1.getValue().getDocumentFrequency(), e2.getValue().getDocumentFrequency());
			assertEquals(e1.getValue().getFrequency(), e2.getValue().getFrequency());
			assertEquals(e1.getValue().getTermId(), e2.getValue().getTermId());
			assertEquals(e1.getValue().getNumberOfEntries(), e2.getValue().getNumberOfEntries());
			assertEquals(e1.getValue().getMaxFrequencyInDocuments(), e2.getValue().getMaxFrequencyInDocuments());
		}
	
		assertFalse(iter1.hasNext());
		assertFalse(iter2.hasNext());

	}
}













