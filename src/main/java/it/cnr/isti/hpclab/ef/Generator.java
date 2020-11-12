/*
 * Elias-Fano compression for Terrier 5
 *
 * Copyright (C) 2018-2020 Nicola Tonellotto 
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

import it.cnr.isti.hpclab.ef.structures.EFDocumentIndex;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terrier.Version;
import org.terrier.applications.CLITool.CLIParsedCLITool;
import org.terrier.querying.IndexRef;
import org.terrier.structures.IndexFactory;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.IndexUtil;
import org.terrier.structures.indexing.LexiconBuilder;
import org.terrier.utility.ApplicationSetup;

public class Generator 
{
    protected static Logger LOGGER = LoggerFactory.getLogger(Generator.class);
    protected static ProgressBar pb_map;
    
    private final int num_terms;
    
    public static class Command extends CLIParsedCLITool
    {
        @Override
        protected Options getOptions() {
            Options opts = super.getOptions();
            opts.addOption("p", "parallelism", true, "parallelism degree (number of threads)");
            opts.addOption("b", "blocks", false, "use positions in new index");
            return opts;
        }

        @Override
        public int run(CommandLine line) throws Exception {
            Args args = new Args();
            if (line.hasOption("p"))
                args.parallelism = line.getOptionValue("p");
            args.with_pos = line.hasOption("b");
            
            args.index = ApplicationSetup.TERRIER_INDEX_PATH + "/" + ApplicationSetup.TERRIER_INDEX_PREFIX + ".properties";
            
            // TR-523 workaround
            if (line.hasOption("I"))
                args.index = line.getOptionValue("I");
            args.path = line.getArgs()[0];
            args.prefix = line.getArgs()[1];
            return process(args);
        }
        
         @Override
         public String commandname() {
             return "ef-recompress";
         }
    
         @Override
         public String help() {
             return super.help() + "\nrequired arguments: destIndexPath destIndexPrefix\n";
         }

         @Override
         public Set<String> commandaliases() {
             return new HashSet<String>(Arrays.asList("ef-generator"));
         }

         @Override
         public String helpsummary() {
             return "copies an index to make it use elias-fano compression (old index is preserved)";
         }

    }
    
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
        
        @Option(name = "-s", required = false, usage = "Create soft links to meta index files")
        public boolean soft_link = true;
    }
    
    public static void main(String[] argv)
    {
        Args args = new Args();
        CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(90));
        try {
            parser.parseArgument(argv);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            return;
        }
        process(args);
    }
    
    @SuppressWarnings("deprecation")
    public static int process(Args args) 
    {
        IndexOnDisk.setIndexLoadingProfileAsRetrieval(false);
        
        // final String src_index_path = FilenameUtils.getFullPath(args.index);
        // final String src_index_prefix = FilenameUtils.getBaseName(args.index);
        
        IndexRef ref_src = IndexRef.of(args.index);
        IndexRef ref_dst = IndexRef.of(args.path + File.separator + args.prefix + ".properties");
        
        // final String dst_index_path = args.path;
        // final String dst_index_prefix = args.prefix;
        
        final int num_threads = ( (args.parallelism != null && Integer.parseInt(args.parallelism) > 1) 
                                        ? Math.min(ForkJoinPool.commonPool().getParallelism(), Integer.parseInt(args.parallelism)) 
                                        : 1) ;
                
        LOGGER.info("Started " + Generator.class.getSimpleName() + " with parallelism " + num_threads + " (out of " + ForkJoinPool.commonPool().getParallelism() + " max parallelism available)");
        LOGGER.warn("Multi-threaded Elias-Fano compression is experimental - caution advised due to threads competing for available memory! YMMV.");

        long starttime = System.currentTimeMillis();
        
        try {
            Generator generator = new Generator(ref_src, ref_dst);
            
            TermPartition[] partitions = generator.partition(num_threads);
            CompressorMapper mapper = new CompressorMapper(ref_src, ref_dst, args.with_pos);
            CompressorReducer merger = new CompressorReducer(ref_dst, args.with_pos);

            // First we perform reassignment in parallel
            System.out.println("Parallel bitfile compression starting...");
            pb_map = new ProgressBarBuilder()
                    .setInitialMax(generator.num_terms)
                    .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                    .setTaskName("EliasFano compression")
                    .setUpdateIntervalMillis(1000)
                    .showSpeed(new DecimalFormat("#.###"))
                    .build();
            TermPartition[] tmp_partitions = Arrays.stream(partitions).parallel().map(mapper).sorted().toArray(TermPartition[]::new);
            pb_map.stop();
            
            long compresstime = System.currentTimeMillis();
            System.out.println("Parallel bitfile compression completed after " + (compresstime - starttime)/1000 + " seconds");

            System.out.println("Sequential merging starting...");
            // Then we perform merging sequentially in a PRECISE order (if the order is wrong, everything is wrong)
            TermPartition last_partition = Arrays.stream(tmp_partitions).reduce(merger).get();

            long mergetime = System.currentTimeMillis();
            System.out.println("Sequential merging completed after " + (mergetime - compresstime)/1000 + " seconds");
            
            // Eventually, we rename the last merge
            IndexUtil.renameIndex(args.path, last_partition.prefix(), args.path, args.prefix);
            
            IndexOnDisk src_index = (IndexOnDisk) IndexFactory.of(ref_src);
            
            if (IndexOnDisk.getLastIndexLoadError() != null) {
                throw new IllegalArgumentException("Error loading index: " + IndexOnDisk.getLastIndexLoadError());
            }
            
            IndexOnDisk dst_index = IndexOnDisk.createNewIndex(args.path, args.prefix);
            dst_index.close();
            dst_index = IndexOnDisk.createIndex(args.path, args.prefix);
            if (IndexOnDisk.getLastIndexLoadError() != null) {
                throw new IllegalArgumentException("Error loading index: " + IndexOnDisk.getLastIndexLoadError());
            }
            
            EFDocumentIndex.write((org.terrier.structures.DocumentIndex) src_index.getDocumentIndex(), args.path + File.separator + args.prefix + ".sizes");
            // IndexUtil.copyStructure(src_index, dst_index, "document", "document");
            
            if (args.soft_link) {
                for (String file : org.terrier.utility.Files.list(((IndexOnDisk) src_index).getPath())) {
                    if (file.startsWith(((IndexOnDisk)src_index).getPrefix() + "." + "meta" + ".")) {
                        Path dst = Paths.get(
                                ((IndexOnDisk)dst_index).getPath() + "/" + file.replaceFirst(
                                        ((IndexOnDisk) src_index).getPrefix() + "\\.meta", 
                                        ((IndexOnDisk) dst_index).getPrefix() + ".meta"
                                    )
                                );
                        Path src = Paths.get(
                                ((IndexOnDisk)src_index).getPath() + "/" + file
                            );
                        Files.createSymbolicLink(dst, src);
                    }
                }
            } else {
                IndexUtil.copyStructure(src_index, dst_index, "meta", "meta");
            }

            long copytime = System.currentTimeMillis();
            System.out.println("Copying other index structures completed after " + (copytime - mergetime)/1000 + " seconds");
            
            writeProperties(src_index, dst_index, args.with_pos);
            LexiconBuilder.optimise(dst_index, "lexicon");

            long opttime = System.currentTimeMillis();
            System.out.println("Lexicon optimization completed after " + (opttime - copytime)/1000 + " seconds");

            dst_index.close();
            src_index.close();
            
            System.out.println("Parallel Elias-Fano compression completed after " + (opttime - starttime)/1000 + " seconds, using "  + num_threads + " threads");
            System.out.println("Final index is at " + args.path + " with prefix " + args.prefix);
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
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
                "index.meta.value-lengths",
                "termpipelines"} )
        {
            dst_index.setIndexProperty(property, src_index.getIndexProperty(property, null));
        }

        dst_index.setIndexProperty("index.terrier.version", Version.VERSION);

        dst_index.setIndexProperty("num.Documents", Integer.toString(src_index.getCollectionStatistics().getNumberOfDocuments()));
        dst_index.setIndexProperty("num.Terms",     Integer.toString(src_index.getCollectionStatistics().getNumberOfUniqueTerms()));
        dst_index.setIndexProperty("num.Pointers",  Long.toString(src_index.getCollectionStatistics().getNumberOfPointers()));
        dst_index.setIndexProperty("num.Tokens",    Long.toString(src_index.getCollectionStatistics().getNumberOfTokens()));
        
        dst_index.setIndexProperty(EliasFano.LOG2QUANTUM, Integer.toString( Integer.parseInt(System.getProperty(EliasFano.LOG2QUANTUM, "8"))));
        dst_index.setIndexProperty(EliasFano.BYTEORDER,   ByteOrder.nativeOrder().toString());
        
        dst_index.setIndexProperty("max.term.length",Integer.toString(ApplicationSetup.MAX_TERM_LENGTH));
        
        dst_index.setIndexProperty("index.lexicon.termids", "aligned");
        dst_index.setIndexProperty("index.lexicon.bsearchshortcut", "default");

        dst_index.setIndexProperty("index.lexicon.class",             "org.terrier.structures.FSOMapFileLexicon");
        dst_index.setIndexProperty("index.lexicon.parameter_types",  "java.lang.String,org.terrier.structures.IndexOnDisk");
        dst_index.setIndexProperty("index.lexicon.parameter_values", "structureName,index");

        dst_index.setIndexProperty("index.lexicon-inputstream.class",             "org.terrier.structures.FSOMapFileLexicon$MapFileLexiconIterator");
        dst_index.setIndexProperty("index.lexicon-inputstream.parameter_types",  "java.lang.String,org.terrier.structures.IndexOnDisk");
        dst_index.setIndexProperty("index.lexicon-inputstream.parameter_values", "structureName,index");
                
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

        dst_index.setIndexProperty("index.document-inputstream.class",            "it.cnr.isti.hpclab.ef.structures.EFDocumentIndex$InputIterator");
        dst_index.setIndexProperty("index.document-inputstream.parameter_types",  "org.terrier.structures.IndexOnDisk");
        dst_index.setIndexProperty("index.document-inputstream.parameter_values", "index");
        
        dst_index.setIndexProperty("index.inverted.class",               "it.cnr.isti.hpclab.ef.structures.EFInvertedIndex");
        dst_index.setIndexProperty("index.inverted.parameter_types",  "org.terrier.structures.IndexOnDisk,org.terrier.structures.DocumentIndex");
        dst_index.setIndexProperty("index.inverted.parameter_values", "index,document");
        
        dst_index.setIndexProperty("index.inverted-inputstream.class",               "it.cnr.isti.hpclab.ef.structures.EFInvertedIndex$InputIterator");
        dst_index.setIndexProperty("index.inverted-inputstream.parameter_types",  "org.terrier.structures.IndexOnDisk");
        dst_index.setIndexProperty("index.inverted-inputstream.parameter_values", "index");
        
        if (with_pos) {
            dst_index.setIndexProperty(EliasFano.HAS_POSITIONS, "true");
        }
        dst_index.flush();
    }
    
    public Generator(final IndexRef src_ref, final IndexRef dst_ref) throws Exception 
    {    
        // Load input index
    	IndexOnDisk src_index = (IndexOnDisk) IndexFactory.of(src_ref);
    	
        if (IndexOnDisk.getLastIndexLoadError() != null) {
            throw new IllegalArgumentException("Error loading index: " + IndexOnDisk.getLastIndexLoadError());
        }
        this.num_terms = src_index.getCollectionStatistics().getNumberOfUniqueTerms();
        src_index.close();
        LOGGER.info("Input index contains " + this.num_terms + " terms");
        
        // check dst index does not exist
        final String dst_index_path = FilenameUtils.getFullPath(dst_ref.toString());
        
        if (!Files.exists(Paths.get(dst_index_path))) {
            LOGGER.info("Index directory " + dst_index_path + " does not exist. It is being created.");
            Files.createDirectories(Paths.get(dst_index_path));
        } else if (Files.exists(Paths.get(dst_ref.toString()))) {
            throw new IllegalArgumentException("Index directory " + dst_index_path + " already contains an index with the given prefix");
        }        
    }

    public TermPartition[] partition(final int num_threads)
    {
        return TermPartition.split(num_terms, num_threads);
    }
}