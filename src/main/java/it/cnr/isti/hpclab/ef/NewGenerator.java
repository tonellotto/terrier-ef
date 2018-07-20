package it.cnr.isti.hpclab.ef;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

import org.apache.commons.io.FilenameUtils;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;

public class NewGenerator 
{
	private static final class TermPartition
	{
		public int begin;
		public int end;
		
		public TermPartition(final int begin, final int end)
		{
			this.begin = begin;
			this.end = end;
		}
		
		public String toString()
		{
			return "[" + begin + "," + end +"]";
		}
		
		public static TermPartition[] split(final int max, final int bins)
		{
			TermPartition[] res = new TermPartition[bins];
			for (int i = 0; i < bins; ++i)
				res[i] = new TermPartition(max * i / bins, max * (i+1) / bins);

			return res;
		}
	}
	
	private static class Mapper implements Function<TermPartition,String> 
	{
		private Args args;
		
		public Mapper(Args args)
		{
			this.args = args;	
		}
		
		@Override
		public String apply(TermPartition terms) 
		{
			String this_prefix = args.prefix + "_partition_" + terms.begin;
			BasicCompressor bc = new BasicCompressor(Index.createIndex(FilenameUtils.getFullPath(args.index), FilenameUtils.getBaseName(args.index)), Paths.get(args.path).toAbsolutePath().toString(), this_prefix);
			try {
				bc.compress(terms.begin, terms.end);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return this_prefix;
		}	
	}

	private static class Sorter implements Comparator<String>
	{
		@Override
		public int compare(String t, String u) 
		{
			int first_termid  = Integer.parseInt(t.substring(t.lastIndexOf("_") + 1));
			int second_termid = Integer.parseInt(t.substring(u.lastIndexOf("_") + 1));

			return Integer.compare(first_termid, second_termid);
		}
	}

	protected static Logger LOGGER = LoggerFactory.getLogger(NewGenerator.class);

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
	}

	public NewGenerator(final NewGenerator.Args args) throws Exception 
	{	
		// Load input index
		final String index_path = FilenameUtils.getFullPath(args.index);
		final String index_prefix = FilenameUtils.getBaseName(args.index);
		
		IndexOnDisk src_index = Index.createIndex(index_path, index_prefix);
		if (Index.getLastIndexLoadError() != null) {
			throw new RuntimeException("Error loading index: " + Index.getLastIndexLoadError());
		}
		this.num_terms = src_index.getCollectionStatistics().getNumberOfUniqueTerms();
		src_index.close();
		LOGGER.info("Input index contains " + this.num_terms + " terms");
		
		// check dst index does not exist 
		if (!Files.exists(Paths.get(args.path))) {
			LOGGER.info("Index directory " + args.path + " does not exist. It is being created.");
			Files.createDirectories(Paths.get(args.path));
		} else if (Files.exists(Paths.get(args.path + File.separator + args.prefix + ".properties"))) {
			throw new RuntimeException("Index directory " + args.path + " already contains an index with prefix " + args.prefix);
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
		
		int num_threads = Math.min(ForkJoinPool.commonPool().getParallelism(), 10);
		
		if (args.parallelism != null) {
			num_threads = Math.min(num_threads, Integer.parseInt(args.parallelism));
		}
		
		if (num_threads < 1)
			num_threads = 1;
		
		LOGGER.info("Started " + NewGenerator.class.getSimpleName() + " with parallelism " + num_threads + " (out of " + ForkJoinPool.commonPool().getParallelism() + " max parallelism available)");
		LOGGER.warn("Multi-threaded docid reassignment is experimental - caution advised due to threads competing for available memory! YMMV.");

		long starttime = System.currentTimeMillis();
		
		try {
			NewGenerator generator = new NewGenerator(args);
			
			TermPartition[] partitions = generator.partition(num_threads);
			Mapper mapper = new Mapper(args);
			Sorter sorter = new Sorter();
			//Reducer merger = new Reducer(args);

			// First we perform reassignment in parallel
			String[] tmp_prefixes = Arrays.stream(partitions).parallel().map(mapper).sorted(sorter).toArray(String[]::new);
			// System.err.println(String.join(",", tmp_prefixes));
			
			// Then we perform merging sequentially in a PRECISE order (if the order is wrong, everything is wrong)
			// String last_prefix = Arrays.stream(tmp_prefixes).reduce(merger).get();
			// System.err.println(last_prefix);

			// Eventually, we rename the last merge
			// IndexUtil.renameIndex(args.output_path, last_prefix, args.output_path, args.output_prefix);
			
			/*
			IndexOnDisk src_index = Index.createIndex(FilenameUtils.getFullPath(args.index), FilenameUtils.getBaseName(args.index));
			IndexOnDisk dst_index = Index.createIndex(args.output_path, args.output_prefix);
			
			LexiconBuilder.optimise(dst_index, "lexicon");
			IndexUtil.copyStructure(src_index, dst_index, "document", "document");
			IndexUtil.copyStructure(src_index, dst_index, "meta", "meta");
			Files.copy(Paths.get(args.map), Paths.get(args.output_path + File.separator + args.output_prefix + ".docid.remap"));
			
			writeProperties(src_index, dst_index);
			
			src_index.close();
			dst_index.close();
			*/
			LOGGER.info("Parallel docid reassignment completed after " + (System.currentTimeMillis() - starttime)/1000 + " seconds, using "  + num_threads + " threads");
			LOGGER.info("Final index is at " + args.path + " with prefix " + args.prefix);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public TermPartition[] partition(final int num_threads)
	{
		return TermPartition.split(num_terms, num_threads);
	}

}
