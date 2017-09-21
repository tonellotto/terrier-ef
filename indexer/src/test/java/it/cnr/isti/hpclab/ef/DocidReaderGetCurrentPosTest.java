package it.cnr.isti.hpclab.ef;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;

import it.cnr.isti.hpclab.ef.structures.EFBasicIterablePosting;
import it.cnr.isti.hpclab.ef.structures.EFLexiconEntry;

public class DocidReaderGetCurrentPosTest extends ApplicationSetupTest
{
	protected IndexOnDisk originalIndex = null;
	protected IndexOnDisk succinctIndex = null;
	
	@Before 
	public void createIndex() throws Exception
	{
		super.doShakespeareIndexing();
		originalIndex = Index.createIndex();
		
		String args[] = new String[2];
		args[0] = originalIndex.getPath();
		args[1] = originalIndex.getPrefix();
		Generator.LOG2QUANTUM = 3;
		Generator.main(args);
		
		succinctIndex = Index.createIndex(args[0], args[1] + EliasFano.USUAL_EXTENSION);
		// System.out.println(succinctIndex.getIndexProperty("log2Quantum", ""));
	}
	
	@After 
	public void deleteIndex() throws IOException
	{
		originalIndex.close();
		succinctIndex.close();
	} 

	@Test
	public void testGetCurrentPos() throws IOException
	{
		Iterator<Entry<String, LexiconEntry>> lin = succinctIndex.getLexicon().iterator();
		
        Map.Entry<String, LexiconEntry> le = null;
        EFLexiconEntry le_in = null;

        int num_tests = 0;
        int docids[] = null;
        while (lin.hasNext()) {
        	le = lin.next();
        	le_in = (EFLexiconEntry) le.getValue();
        	if (le_in.getDocumentFrequency() >= 1) {
            	docids = new int[le_in.getDocumentFrequency()];
            	EFBasicIterablePosting p = (EFBasicIterablePosting) succinctIndex.getInvertedIndex().getPostings(le_in);
            	int pos = 0;
            	while (p.next() != IterablePosting.END_OF_LIST) {
            		docids[pos++] = p.getId();
            	}
            	p.close();
            	
            	p = (EFBasicIterablePosting) succinctIndex.getInvertedIndex().getPostings(le_in);
            	
            	for (int i = 0; i < docids.length; i += 3) {
            		p.next(docids[i]);
            		assertEquals(i, p.getCurrentDocidPosition());
            	}
            	p.close();            	
            	num_tests++;
        	}
        }
        System.err.println("Tested " + num_tests + " posting lists");
	}
}
