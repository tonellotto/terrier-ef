package it.cnr.isti.hpclab;

import it.unimi.di.big.mg4j.document.CompositeDocumentFactory;
import it.unimi.di.big.mg4j.document.DocumentFactory;
import it.unimi.di.big.mg4j.document.DocumentSequence;
import it.unimi.di.big.mg4j.document.HtmlDocumentFactory;
import it.unimi.di.big.mg4j.document.IdentityDocumentFactory;
import it.unimi.di.big.mg4j.document.TRECDocumentCollection;
import it.unimi.di.big.mg4j.document.TRECHeaderDocumentFactory;
import it.unimi.di.big.mg4j.index.NullTermProcessor;
import it.unimi.di.big.mg4j.index.TermProcessor;
import it.unimi.di.big.mg4j.tool.IndexBuilder;
import it.unimi.di.big.mg4j.tool.Scan;
import it.unimi.dsi.fastutil.io.BinIO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class Indexer 
{
	static final Logger logger = LoggerFactory.getLogger(Indexer.class);
	static List<String> list = new ArrayList<String>();
			
	private static void fileList(File dir)
	{
        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; i++){
            if (files[i].isDirectory())
                fileList(files[i]);
            else {
            	if (files[i].getName().endsWith(".gz") && files[i].getName().startsWith("B")) 
            		list.add(files[i].getAbsolutePath());
            }
        }
    }
	
	private static void generateSequence(String rootDir, String sequence) throws ConfigurationException, IOException
	{
		fileList(new File(rootDir));
		String[] files = list.toArray(new String[0]);
		
		System.out.println(list.size());
	

		String[] properties = { "encoding=UTF-8" };
		
		DocumentFactory userFactory = new HtmlDocumentFactory(properties); 
		DocumentFactory composite = CompositeDocumentFactory.getFactory( new TRECHeaderDocumentFactory(), userFactory );

		BinIO.storeObject( new TRECDocumentCollection( files, composite, 64*1024, true ), sequence );
	}
	
	
	public static void main(String[] args) throws Exception 
	{
		String rootDir = "/Users/khast/wt10g/";
		String sequence = "/Users/khast/index-mg4j/wt10g.collection";
		
		// generateSequence(rootDir, sequence);
		
		
		File directory = new File("/Users/khast/index-mg4j");
		String basename = "wt10g";
		int documentsPerBatch = Scan.DEFAULT_BATCH_SIZE;
		TermProcessor termProcessor = NullTermProcessor.getInstance();

		DocumentSequence documentSequence;
        documentSequence = Scan.getSequence(sequence, IdentityDocumentFactory.class, new String[] {}, Scan.DEFAULT_DELIMITER, logger);

        logger.info(String.format("Term processor class is %s", termProcessor.getClass()));
        
        IndexBuilder indexBuilder = new IndexBuilder(new File(directory,basename).getAbsolutePath(), documentSequence);
        indexBuilder.termProcessor(termProcessor).documentsPerBatch(documentsPerBatch);

        indexBuilder.run();
	}

}
