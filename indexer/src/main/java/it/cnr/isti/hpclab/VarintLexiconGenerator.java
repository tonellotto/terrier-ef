package it.cnr.isti.hpclab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
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
import org.terrier.structures.LexiconOutputStream;

import it.cnr.isti.hpclab.ef.structures.EFBlockLexiconEntry;
import it.cnr.isti.hpclab.ef.structures.EFLexiconEntry;
import it.cnr.isti.hpclab.structures.VarintBlockLexiconEntry;
import it.cnr.isti.hpclab.structures.VarintLexicon;
import it.cnr.isti.hpclab.structures.VarintLexiconEntry;
import it.cnr.isti.hpclab.structures.VarintLexiconOutputStream;

import it.unimi.dsi.logging.ProgressLogger;


public class VarintLexiconGenerator 
{
	private static final Logger LOGGER = LoggerFactory.getLogger(VarintLexiconGenerator.class);
	private static final ProgressLogger pl = new ProgressLogger(LOGGER, 5, TimeUnit.SECONDS, "terms");

	public static void main(String[] args) throws IOException
	{
		// Parameters
		String src_index_path = null, src_index_prefix = null;
		boolean has_blocks = false;
		boolean overwrite_properties = false;
		
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

		options.addOption( Option.builder("b")
				.longOpt("blocks")
				.desc("If present, the source lexicon includes positional information, a.k.a. blocks.")
				.hasArg(false)
//				.required()
				.build());

		options.addOption( Option.builder("o")
				.longOpt("overwrite_properties")
				.desc("If present, the original properties file will be overwritten to use the new lexicon")
				.hasArg(false)
//				.required()
				.build());

		try {
			
			CommandLineParser parser = new DefaultParser();

		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );

		    if (line.hasOption("help")) {
		    	String header = "";
		    	String footer = "This command line util creates a new version of the EF terrier lexicon,\n"+
		    					"using varint to compress the data structures. The input lexicon file\n" + 
		    					"(ending with \".lexicon.fsomapfile\") will be parsed, and two new lexicon\n" +
		    					"files are created, ending with \".lexicon" + VarintLexicon.LEXICON_DATA + "\" and \"\".lexicon" + VarintLexicon.LEXICON_OFFSETS + "\".\n" + 
		    					"Moreover, the properties files will be modified with the following *commented* lines, to use the new lexicon:";
		    	formatter.printHelp( "java " + VarintLexiconGenerator.class.getName(), header, options , footer);
		    	System.exit(0);
		    }
		    
		    // Mandatory arguments
		    src_index_path   = line.getOptionValue("src_path");
		    src_index_prefix = line.getOptionValue("src_prefix");		
		    
		    // Optional arguments
		    has_blocks = line.hasOption("blocks");
		    overwrite_properties = line.hasOption("overwrite_properties");
		} catch (Exception exp) {
		    System.out.println( "Unexpected exception: " + exp.getMessage() );
		    formatter.printHelp( "java " + VarintLexiconGenerator.class.getName(), options );
		}
		
		
	    IndexOnDisk srcIndex = Index.createIndex(src_index_path, src_index_prefix);
		if (Index.getLastIndexLoadError() != null) {
			System.err.println(Index.getLastIndexLoadError());
			System.exit(-2);
		}			
		
        pl.expectedUpdates = srcIndex.getCollectionStatistics().getNumberOfUniqueTerms();
        pl.displayFreeMemory = false;
        pl.displayLocalSpeed = true;
		
        if (has_blocks) {
        	LexiconOutputStream<String> los = new VarintLexiconOutputStream(src_index_path, src_index_prefix);				
        	Iterator<Entry<String, LexiconEntry>> lin = srcIndex.getLexicon().iterator();
		
        	Map.Entry<String, LexiconEntry> le = null;
        	EFBlockLexiconEntry ble_in = null;
        	VarintBlockLexiconEntry ble_out = null;
		
        	pl.start("Converting lexicon...");
        	while (lin.hasNext()) {
        		le = lin.next();
        		ble_in = (EFBlockLexiconEntry) le.getValue();			
        		ble_out = new VarintBlockLexiconEntry(ble_in.getTermId(), ble_in.getDocumentFrequency(), ble_in.getFrequency(),
											 		  ble_in.getDocidOffset(), ble_in.getFreqOffset(), 
											 		  ble_in.getPosOffset(), ble_in.getSumsMaxPos());
        			
        		los.writeNextEntry(le.getKey(), ble_out);
        		pl.update();
        	}
        	pl.stop("End of processing!");
        
        	los.close();
        
        	if (overwrite_properties)
        		overwriteVarintBlockLexiconProperties(srcIndex);
        	else
        		updateVarintBlockLexiconProperties(srcIndex);
        	
        } else {
        	
        	LexiconOutputStream<String> los = new VarintLexiconOutputStream(src_index_path, src_index_prefix);				
        	Iterator<Entry<String, LexiconEntry>> lin = srcIndex.getLexicon().iterator();
		
        	Map.Entry<String, LexiconEntry> le = null;
        	EFLexiconEntry le_in = null;
        	VarintLexiconEntry le_out = null;
		
        	pl.start("Converting lexicon...");
        	while (lin.hasNext()) {
        		le = lin.next();
        		le_in = (EFLexiconEntry) le.getValue();			
        		le_out = new VarintLexiconEntry(le_in.getTermId(), le_in.getDocumentFrequency(), le_in.getFrequency(),
											 	le_in.getDocidOffset(), le_in.getFreqOffset());
        		los.writeNextEntry(le.getKey(), le_out);
        		pl.update();
        	}
        	pl.stop("End of processing!");
        
        	los.close();
        
        	if (overwrite_properties)
        		overwriteVarintLexiconProperties(srcIndex);
        	else
        		updateVarintLexiconProperties(srcIndex);
        }
        
        srcIndex.close();
	}

	private static void updateVarintLexiconProperties(final IndexOnDisk srcIndex) 
	{
		String filename = srcIndex.getPath() + File.separator + srcIndex.getPrefix() + ".properties";
		
		String content = "\n" + 
						 "#index.lexicon.class=it.cnr.isti.hpclab.structures.VarintLexicon\n" +
						 "#index.lexicon.parameter_types=org.terrier.structures.IndexOnDisk\n" +
						 "#index.lexicon.parameter_values=index\n" +
						 "\n" +
						 "#index.lexicon-valuefactory.class=it.cnr.isti.hpclab.structures.VarintLexiconEntry$Factory\n" +
						 "#index.lexicon-valuefactory.parameter_values=\n" +
						 "#index.lexicon-valuefactory.parameter_types=\n";
		try {
		    Files.write(Paths.get(filename), content.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	private static void updateVarintBlockLexiconProperties(final IndexOnDisk srcIndex) 
	{
		String filename = srcIndex.getPath() + File.separator + srcIndex.getPrefix() + ".properties";
		
		String content = "\n" + 
						 "#index.lexicon.class=it.cnr.isti.hpclab.structures.VarintLexicon\n" +
						 "#index.lexicon.parameter_types=org.terrier.structures.IndexOnDisk\n" +
						 "#index.lexicon.parameter_values=index\n" +
						 "\n" +
						 "#index.lexicon-valuefactory.class=it.cnr.isti.hpclab.structures.VarintBlockLexiconEntry$Factory\n" +
						 "#index.lexicon-valuefactory.parameter_values=\n" +
						 "#index.lexicon-valuefactory.parameter_types=\n";
		try {
		    Files.write(Paths.get(filename), content.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	public static void overwriteVarintLexiconProperties(final IndexOnDisk srcIndex) throws IOException
	{
		String filename = srcIndex.getPath() + File.separator + srcIndex.getPrefix() + ".properties";
		
		
		FileInputStream in = new FileInputStream(filename);
		Properties properties = new Properties();
		properties.load(in);
		in.close();

		properties.remove("index.lexicon.termids");
		properties.remove("index.lexicon.bsearchshortcut");
		
		properties.remove("index.lexicon.class");
		properties.remove("index.lexicon.parameter_types");
		properties.remove("index.lexicon.parameter_values");
		
		properties.remove("index.lexicon-keyfactory.class");
		properties.remove("index.lexicon-keyfactory.parameter_types");
		properties.remove("index.lexicon-keyfactory.parameter_values");
		
		properties.remove("index.lexicon-valuefactory.class");
		properties.remove("index.lexicon-valuefactory.parameter_values");
		properties.remove("index.lexicon-valuefactory.parameter_types");
		
		properties.setProperty("index.lexicon.class",			 "it.cnr.isti.hpclab.structures.VarintLexicon");
		properties.setProperty("index.lexicon.parameter_types",	 "org.terrier.structures.IndexOnDisk");
		properties.setProperty("index.lexicon.parameter_values", "index");
		
		properties.setProperty("index.lexicon-valuefactory.class",			  "it.cnr.isti.hpclab.structures.VarintLexiconEntry$Factory");
		properties.setProperty("index.lexicon-valuefactory.parameter_values", "");
		properties.setProperty("index.lexicon-valuefactory.parameter_types",  "");
		
		FileOutputStream out = new FileOutputStream(filename);
		properties.store(out, null);
		out.close();
	}
	
	public static void overwriteVarintBlockLexiconProperties(final IndexOnDisk srcIndex) throws IOException
	{
		String filename = srcIndex.getPath() + File.separator + srcIndex.getPrefix() + ".properties";
		
		
		FileInputStream in = new FileInputStream(filename);
		Properties properties = new Properties();
		properties.load(in);
		in.close();

		properties.remove("index.lexicon.termids");
		properties.remove("index.lexicon.bsearchshortcut");
		
		properties.remove("index.lexicon.class");
		properties.remove("index.lexicon.parameter_types");
		properties.remove("index.lexicon.parameter_values");
		
		properties.remove("index.lexicon-keyfactory.class");
		properties.remove("index.lexicon-keyfactory.parameter_types");
		properties.remove("index.lexicon-keyfactory.parameter_values");
		
		properties.remove("index.lexicon-valuefactory.class");
		properties.remove("index.lexicon-valuefactory.parameter_values");
		properties.remove("index.lexicon-valuefactory.parameter_types");
		
		properties.setProperty("index.lexicon.class",			 "it.cnr.isti.hpclab.structures.VarintLexicon");
		properties.setProperty("index.lexicon.parameter_types",	 "org.terrier.structures.IndexOnDisk");
		properties.setProperty("index.lexicon.parameter_values", "index");
		
		properties.setProperty("index.lexicon-valuefactory.class",			  "it.cnr.isti.hpclab.structures.VarintBlockLexiconEntry$Factory");
		properties.setProperty("index.lexicon-valuefactory.parameter_values", "");
		properties.setProperty("index.lexicon-valuefactory.parameter_types",  "");
		
		FileOutputStream out = new FileOutputStream(filename);
		properties.store(out, null);
		out.close();
	}

}