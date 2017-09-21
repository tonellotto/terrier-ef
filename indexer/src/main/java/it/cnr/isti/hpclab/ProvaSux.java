package it.cnr.isti.hpclab;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.LexiconEntry;

import it.cnr.isti.hpclab.ef.structures.EFLexiconEntry;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

import it.unimi.dsi.logging.ProgressLogger;

import it.unimi.dsi.sux4j.util.EliasFanoMonotoneLongBigList;

public class ProvaSux 
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ProvaSux.class);
	private static final ProgressLogger pl = new ProgressLogger(LOGGER, 5, TimeUnit.SECONDS, "terms");

	public static void main(String[] args) throws IOException
	{
		// Parameters
		String src_index_path = null, src_index_prefix = null;
		
		// create Options object
		Options options = new Options();
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		
		options.addOption( "h", "help", false, "Print help messages." );
		
		options.addOption( Option.builder("d")
				.longOpt("src_path")
				.desc("Path of the input Terrier index")
				.hasArg()
				.argName("PATHNAME")
				.required()
				.build());
		
		options.addOption( Option.builder("i")
				.longOpt("src_prefix")
				.desc("Prefix of the input Terrier index")
				.hasArg()
				.argName("STRING")
				.required()
				.build());

		try {
			
			CommandLineParser parser = new DefaultParser();

		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );

		    if (line.hasOption("help")) {
		    	String header = "";
		    	String footer = "";
		    	formatter.printHelp( "java " + ProvaSux.class.getName(), header, options , footer);
		    	System.exit(0);
		    }
		    
		    // Mandatory arguments
		    src_index_path   = line.getOptionValue("src_path");
		    src_index_prefix = line.getOptionValue("src_prefix");		
		    
		} catch (Exception exp) {
		    System.out.println( "Unexpected exception: " + exp.getMessage() );
		    formatter.printHelp( "java " + ProvaSux.class.getName(), options );
		}
		
	    IndexOnDisk srcIndex = Index.createIndex(src_index_path, src_index_prefix);
		if (Index.getLastIndexLoadError() != null) {
			System.err.println(Index.getLastIndexLoadError());
			System.exit(-2);
		}			
		
        pl.expectedUpdates = srcIndex.getCollectionStatistics().getNumberOfUniqueTerms();
        pl.displayFreeMemory = false;
        pl.displayLocalSpeed = true;
		
       					
        Iterator<Entry<String, LexiconEntry>> lin = srcIndex.getLexicon().iterator();
		
        Map.Entry<String, LexiconEntry> le = null;
        EFLexiconEntry le_in = null;
		
        LongList l = new LongArrayList();
        
        pl.start("Converting lexicon...");
        while (lin.hasNext()) {
        	le = lin.next();
        	le_in = (EFLexiconEntry) le.getValue();
        	l.add(le_in.getDocidOffset());
        	pl.update();
        }
        pl.stop("End of processing!");
        
        srcIndex.close();
        
        EliasFanoMonotoneLongBigList efl = new EliasFanoMonotoneLongBigList(l);
        // save the object to file
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            fos = new FileOutputStream("del.me.oos");
            out = new ObjectOutputStream(fos);
            out.writeObject(efl);

            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
	}
}