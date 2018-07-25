/*
 * Elias-Fano compression for Terrier 5
 *
 * Copyright (C) 2018-2018 Nicola Tonellotto 
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */
package it.cnr.isti.hpclab.ef;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;

import org.apache.commons.io.FilenameUtils;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.IndexUtil;
import org.terrier.structures.indexing.LexiconBuilder;
import org.terrier.utility.ApplicationSetup;

import it.cnr.isti.hpclab.ef.structures.EFDocumentIndex;

public class Generator 
{
	protected static Logger LOGGER = LoggerFactory.getLogger(Generator.class);

	private final int num_terms;
	
	public static final class Args 
	{
	    // required arguments

	    @Option(name = "-index",  metaVar = "[String]", required = true, usage = "Input Index")
	    public String index;

	    @Option(name = "-path",  metaVar = "[Directory]", required = true, usage = "Terrier index path")
	    public String path;

	    @Option(name = "-prefix", metaVar = "[String]", required = true, usage = "Terrier index prefix")
	    public String prefix;

	    // optional arguments
	    
	    @Option(name = "-p", metaVar = "[Number]", required = false, usage = "Parallelism degree")
	    public String parallelism;
	    
	    @Option(name = "-b", required = false, usage = "Compress positions with Elias-Fano")
	    public boolean with_pos = false;

	}

	public Generator(final String src_index_path, final String src_index_prefix, final String dst_index_path, final String dst_index_prefix) throws Exception 
	{	
		// Load input index
		IndexOnDisk src_index = Index.createIndex(src_index_path, src_index_prefix);
		if (Index.getLastIndexLoadError() != null) {
			throw new RuntimeException("Error loading index: " + Index.getLastIndexLoadError());
		}
		this.num_terms = src_index.getCollectionStatistics().getNumberOfUniqueTerms();
		src_index.close();
		LOGGER.info("Input index contains " + this.num_terms + " terms");
		
		// check dst index does not exist 
		if (!Files.exists(Paths.get(dst_index_path))) {
			LOGGER.info("Index directory " + dst_index_path + " does not exist. It is being created.");
			Files.createDirectories(Paths.get(dst_index_path));
		} else if (Files.exists(Paths.get(dst_index_path + File.separator + dst_index_prefix + ".properties"))) {
			throw new RuntimeException("Index directory " + dst_index_path + " already contains an index with prefix " + dst_index_prefix);
		}		
	}
	
	public static void main(String[] argv)
	{
		IndexOnDisk.setIndexLoadingProfileAsRetrieval(false);
		
		Args args = new Args();
		CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(90));
		try {
			parser.parseArgument(argv);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			parser.printUsage(System.err);
			return;
		}
		
		final String src_index_path = FilenameUtils.getFullPath(args.index);
		final String src_index_prefix = FilenameUtils.getBaseName(args.index);
		
		final String dst_index_path = args.path;
		final String dst_index_prefix = args.prefix;
		
		final int num_threads = ( (args.parallelism != null && Integer.parseInt(args.parallelism) > 1) 
										? Math.min(ForkJoinPool.commonPool().getParallelism(), Integer.parseInt(args.parallelism)) 
										: 1) ;
				
		LOGGER.info("Started " + Generator.class.getSimpleName() + " with parallelism " + num_threads + " (out of " + ForkJoinPool.commonPool().getParallelism() + " max parallelism available)");
		LOGGER.warn("Multi-threaded Elias-Fano compression is experimental - caution advised due to threads competing for available memory! YMMV.");

		long starttime = System.currentTimeMillis();
		
		try {
			Generator generator = new Generator(src_index_path, src_index_prefix, dst_index_path, dst_index_prefix);
			
			TermPartition[] partitions = generator.partition(num_threads);
			CompressorMapper mapper = new CompressorMapper(src_index_path, src_index_prefix, dst_index_path, dst_index_prefix, args.with_pos);
			CompressorReducer3 merger = new CompressorReducer3(dst_index_path, dst_index_prefix, args.with_pos);

			// Arrays.stream(partitions).parallel().map(mapper).sorted().reduce(merger);
			// First we perform reassignment in parallel
			TermPartition[] tmp_partitions = Arrays.stream(partitions).parallel().map(mapper).sorted().toArray(TermPartition[]::new);
			// System.err.println(String.join(",", Arrays.stream(tmp_partitions).map(x -> x.prefix()).toArray(String[]::new)));
			
			long compresstime = System.currentTimeMillis();
			LOGGER.info("Parallel bitfile compression completed after " + (compresstime - starttime)/1000 + " seconds");
			
			// Then we perform merging sequentially in a PRECISE order (if the order is wrong, everything is wrong)
			TermPartition last_partition = Arrays.stream(tmp_partitions).reduce(merger).get();
			// System.err.println(last_partition.prefix());
			
			long mergetime = System.currentTimeMillis();
			LOGGER.info("Sequential merging completed after " + (mergetime - compresstime)/1000 + " seconds");
			
			// Eventually, we rename the last merge
			IndexUtil.renameIndex(args.path, last_partition.prefix(), dst_index_path, dst_index_prefix);
			
			IndexOnDisk src_index = Index.createIndex(src_index_path, src_index_prefix);
			if (Index.getLastIndexLoadError() != null) {
				throw new RuntimeException("Error loading index: " + Index.getLastIndexLoadError());
			}
			
			IndexOnDisk dst_index = Index.createNewIndex(dst_index_path, dst_index_prefix);
			dst_index.close();
			dst_index = Index.createIndex(dst_index_path, dst_index_prefix);
			if (Index.getLastIndexLoadError() != null) {
				throw new RuntimeException("Error loading index: " + Index.getLastIndexLoadError());
			}
			
			EFDocumentIndex.write((org.terrier.structures.DocumentIndex) src_index.getDocumentIndex(), dst_index_path + File.separator + dst_index_prefix + ".sizes");
			// IndexUtil.copyStructure(src_index, dst_index, "document", "document");
			IndexUtil.copyStructure(src_index, dst_index, "meta", "meta");

			long copytime = System.currentTimeMillis();
			LOGGER.info("Copying other index structures completed after " + (copytime - mergetime)/1000 + " seconds");
			
			writeProperties(src_index, dst_index, args.with_pos);
			LexiconBuilder.optimise(dst_index, "lexicon");

			long opttime = System.currentTimeMillis();
			LOGGER.info("Lexicon optimization completed after " + (opttime - copytime)/1000 + " seconds");

			dst_index.close();
			src_index.close();
			
			LOGGER.info("Parallel Elias-Fano compression completed after " + (opttime - starttime)/1000 + " seconds, using "  + num_threads + " threads");
			LOGGER.info("Final index is at " + args.path + " with prefix " + args.prefix);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void writeProperties(IndexOnDisk src_index, IndexOnDisk dst_index, boolean with_pos) throws IOException 
	{	
		for (String property : new String[] {
				"index.meta-inputstream.class",
				"index.meta-inputstream.parameter_types",
				"index.meta-inputstream.parameter_values",
				"index.meta.class",
				"index.meta.compression-level",
				"index.meta.data-source",
				"index.meta.entries",
				"index.meta.entry-length",
				"index.meta.index-source",
				"index.meta.key-names",
				"index.meta.parameter_types",
				"index.meta.parameter_values",
				"index.meta.reverse-key-names",
				"index.meta.value-lengths"} ) 
		{
			dst_index.setIndexProperty(property, src_index.getIndexProperty(property, null));
		}

		dst_index.setIndexProperty("index.terrier.version", "5.0");
		
		dst_index.setIndexProperty("num.Documents", Integer.toString(src_index.getCollectionStatistics().getNumberOfDocuments()));
		dst_index.setIndexProperty("num.Terms",     Integer.toString(src_index.getCollectionStatistics().getNumberOfUniqueTerms()));
		dst_index.setIndexProperty("num.Pointers",  Long.toString(src_index.getCollectionStatistics().getNumberOfPointers()));
		dst_index.setIndexProperty("num.Tokens",    Long.toString(src_index.getCollectionStatistics().getNumberOfTokens()));
		
		dst_index.setIndexProperty(EliasFano.LOG2QUANTUM, Integer.toString( Integer.parseInt(System.getProperty(EliasFano.LOG2QUANTUM, "8"))));
		dst_index.setIndexProperty(EliasFano.BYTEORDER,   ByteOrder.nativeOrder().toString());
		
		dst_index.setIndexProperty("max.term.length",Integer.toString(ApplicationSetup.MAX_TERM_LENGTH));
		
		dst_index.setIndexProperty("index.lexicon.termids", "aligned");
		dst_index.setIndexProperty("index.lexicon.bsearchshortcut", "default");

		dst_index.setIndexProperty("index.lexicon.class",		     "org.terrier.structures.FSOMapFileLexicon");
		dst_index.setIndexProperty("index.lexicon.parameter_types",  "java.lang.String,org.terrier.structures.IndexOnDisk");
		dst_index.setIndexProperty("index.lexicon.parameter_values", "structureName,index");

		dst_index.setIndexProperty("index.lexicon-keyfactory.class",            "org.terrier.structures.seralization.FixedSizeTextFactory");
		dst_index.setIndexProperty("index.lexicon-keyfactory.parameter_types",  "java.lang.String");
		dst_index.setIndexProperty("index.lexicon-keyfactory.parameter_values", "${max.term.length}");
		
		if (!with_pos)
			dst_index.setIndexProperty("index.lexicon-valuefactory.class",            "it.cnr.isti.hpclab.ef.structures.EFLexiconEntry$Factory");
		else
			dst_index.setIndexProperty("index.lexicon-valuefactory.class",            "it.cnr.isti.hpclab.ef.structures.EFBlockLexiconEntry$Factory");
		dst_index.setIndexProperty("index.lexicon-valuefactory.parameter_values", "");
		dst_index.setIndexProperty("index.lexicon-valuefactory.parameter_types",  "");

		dst_index.setIndexProperty("index.document.class",            "it.cnr.isti.hpclab.ef.structures.EFDocumentIndex");
		dst_index.setIndexProperty("index.document.parameter_types",  "org.terrier.structures.IndexOnDisk");
		dst_index.setIndexProperty("index.document.parameter_values", "index");

		dst_index.setIndexProperty("index.inverted.class", 		      "it.cnr.isti.hpclab.ef.structures.EFInvertedIndex");
		dst_index.setIndexProperty("index.inverted.parameter_types",  "org.terrier.structures.IndexOnDisk,org.terrier.structures.DocumentIndex");
		dst_index.setIndexProperty("index.inverted.parameter_values", "index,document");
		
		if (with_pos) {
			dst_index.setIndexProperty(EliasFano.HAS_POSITIONS, "true");
		}
		dst_index.flush();
		
		
	}

	public TermPartition[] partition(final int num_threads)
	{
		return TermPartition.split(num_terms, num_threads);
	}
}