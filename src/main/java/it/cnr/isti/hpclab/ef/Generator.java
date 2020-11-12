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
    protected static ProgressBar pbMap;
    
    private final int numTerms;
    
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
            args.withPos = line.hasOption("b");
            
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
        public boolean withPos = false;
        
        @Option(name = "-s", required = false, usage = "Create soft links to meta index files")
        public boolean softLink = true;
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
        
        IndexRef refSrc = IndexRef.of(args.index);
        IndexRef refDst = IndexRef.of(args.path + File.separator + args.prefix + ".properties");
        
        // final String dst_index_path = args.path;
        // final String dst_index_prefix = args.prefix;
        
        final int numThreads = ( (args.parallelism != null && Integer.parseInt(args.parallelism) > 1) 
                                        ? Math.min(ForkJoinPool.commonPool().getParallelism(), Integer.parseInt(args.parallelism)) 
                                        : 1) ;
                
        LOGGER.info("Started " + Generator.class.getSimpleName() + " with parallelism " + numThreads + " (out of " + ForkJoinPool.commonPool().getParallelism() + " max parallelism available)");
        LOGGER.warn("Multi-threaded Elias-Fano compression is experimental - caution advised due to threads competing for available memory! YMMV.");

        long starttime = System.currentTimeMillis();
        
        try {
            Generator generator = new Generator(refSrc, refDst);
            
            TermPartition[] partitions = generator.partition(numThreads);
            CompressorMapper mapper = new CompressorMapper(refSrc, refDst, args.withPos);
            CompressorReducer merger = new CompressorReducer(refDst, args.withPos);

            // First we perform reassignment in parallel
            System.out.println("Parallel bitfile compression starting...");
            pbMap = new ProgressBarBuilder()
                    .setInitialMax(generator.numTerms)
                    .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                    .setTaskName("EliasFano compression")
                    .setUpdateIntervalMillis(1000)
                    .showSpeed(new DecimalFormat("#.###"))
                    .build();
            TermPartition[] tmpPartitions = Arrays.stream(partitions).parallel().map(mapper).sorted().toArray(TermPartition[]::new);
            pbMap.stop();
            
            long compresstime = System.currentTimeMillis();
            System.out.println("Parallel bitfile compression completed after " + (compresstime - starttime)/1000 + " seconds");

            System.out.println("Sequential merging starting...");
            // Then we perform merging sequentially in a PRECISE order (if the order is wrong, everything is wrong)
            TermPartition last_partition = Arrays.stream(tmpPartitions).reduce(merger).get();

            long mergetime = System.currentTimeMillis();
            System.out.println("Sequential merging completed after " + (mergetime - compresstime)/1000 + " seconds");
            
            // Eventually, we rename the last merge
            IndexUtil.renameIndex(args.path, last_partition.prefix(), args.path, args.prefix);
            
            IndexOnDisk srcIndex = (IndexOnDisk) IndexFactory.of(refSrc);
            
            if (IndexOnDisk.getLastIndexLoadError() != null) {
                throw new IllegalArgumentException("Error loading index: " + IndexOnDisk.getLastIndexLoadError());
            }
            
            IndexOnDisk dstIndex = IndexOnDisk.createNewIndex(args.path, args.prefix);
            dstIndex.close();
            dstIndex = IndexOnDisk.createIndex(args.path, args.prefix);
            if (IndexOnDisk.getLastIndexLoadError() != null) {
                throw new IllegalArgumentException("Error loading index: " + IndexOnDisk.getLastIndexLoadError());
            }
            
            EFDocumentIndex.write((org.terrier.structures.DocumentIndex) srcIndex.getDocumentIndex(), args.path + File.separator + args.prefix + ".sizes");
            // IndexUtil.copyStructure(src_index, dst_index, "document", "document");
            
            if (args.softLink) {
                for (String file : org.terrier.utility.Files.list(((IndexOnDisk) srcIndex).getPath())) {
                    if (file.startsWith(((IndexOnDisk)srcIndex).getPrefix() + "." + "meta" + ".")) {
                        Path dst = Paths.get(
                                ((IndexOnDisk)dstIndex).getPath() + "/" + file.replaceFirst(
                                        ((IndexOnDisk) srcIndex).getPrefix() + "\\.meta", 
                                        ((IndexOnDisk) dstIndex).getPrefix() + ".meta"
                                    )
                                );
                        Path src = Paths.get(
                                ((IndexOnDisk)srcIndex).getPath() + "/" + file
                            );
                        Files.createSymbolicLink(dst, src);
                    }
                }
            } else {
                IndexUtil.copyStructure(srcIndex, dstIndex, "meta", "meta");
            }

            long copytime = System.currentTimeMillis();
            System.out.println("Copying other index structures completed after " + (copytime - mergetime)/1000 + " seconds");
            
            writeProperties(srcIndex, dstIndex, args.withPos);
            LexiconBuilder.optimise(dstIndex, "lexicon");

            long opttime = System.currentTimeMillis();
            System.out.println("Lexicon optimization completed after " + (opttime - copytime)/1000 + " seconds");

            dstIndex.close();
            srcIndex.close();
            
            System.out.println("Parallel Elias-Fano compression completed after " + (opttime - starttime)/1000 + " seconds, using "  + numThreads + " threads");
            System.out.println("Final index is at " + args.path + " with prefix " + args.prefix);
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
    
    private static void writeProperties(IndexOnDisk srcIndex, IndexOnDisk dstIndex, boolean withPos) throws IOException 
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
            dstIndex.setIndexProperty(property, srcIndex.getIndexProperty(property, null));
        }

        dstIndex.setIndexProperty("index.terrier.version", Version.VERSION);

        dstIndex.setIndexProperty("num.Documents", Integer.toString(srcIndex.getCollectionStatistics().getNumberOfDocuments()));
        dstIndex.setIndexProperty("num.Terms",     Integer.toString(srcIndex.getCollectionStatistics().getNumberOfUniqueTerms()));
        dstIndex.setIndexProperty("num.Pointers",  Long.toString(srcIndex.getCollectionStatistics().getNumberOfPointers()));
        dstIndex.setIndexProperty("num.Tokens",    Long.toString(srcIndex.getCollectionStatistics().getNumberOfTokens()));
        
        dstIndex.setIndexProperty(EliasFano.LOG2QUANTUM, Integer.toString( Integer.parseInt(System.getProperty(EliasFano.LOG2QUANTUM, "8"))));
        dstIndex.setIndexProperty(EliasFano.BYTEORDER,   ByteOrder.nativeOrder().toString());
        
        dstIndex.setIndexProperty("max.term.length",Integer.toString(ApplicationSetup.MAX_TERM_LENGTH));
        
        dstIndex.setIndexProperty("index.lexicon.termids", "aligned");
        dstIndex.setIndexProperty("index.lexicon.bsearchshortcut", "default");

        dstIndex.setIndexProperty("index.lexicon.class",             "org.terrier.structures.FSOMapFileLexicon");
        dstIndex.setIndexProperty("index.lexicon.parameter_types",  "java.lang.String,org.terrier.structures.IndexOnDisk");
        dstIndex.setIndexProperty("index.lexicon.parameter_values", "structureName,index");

        dstIndex.setIndexProperty("index.lexicon-inputstream.class",             "org.terrier.structures.FSOMapFileLexicon$MapFileLexiconIterator");
        dstIndex.setIndexProperty("index.lexicon-inputstream.parameter_types",  "java.lang.String,org.terrier.structures.IndexOnDisk");
        dstIndex.setIndexProperty("index.lexicon-inputstream.parameter_values", "structureName,index");
                
        dstIndex.setIndexProperty("index.lexicon-keyfactory.class",            "org.terrier.structures.seralization.FixedSizeTextFactory");
        dstIndex.setIndexProperty("index.lexicon-keyfactory.parameter_types",  "java.lang.String");
        dstIndex.setIndexProperty("index.lexicon-keyfactory.parameter_values", "${max.term.length}");
        
        if (!withPos)
            dstIndex.setIndexProperty("index.lexicon-valuefactory.class",            "it.cnr.isti.hpclab.ef.structures.EFLexiconEntry$Factory");
        else
            dstIndex.setIndexProperty("index.lexicon-valuefactory.class",            "it.cnr.isti.hpclab.ef.structures.EFBlockLexiconEntry$Factory");
        dstIndex.setIndexProperty("index.lexicon-valuefactory.parameter_values", "");
        dstIndex.setIndexProperty("index.lexicon-valuefactory.parameter_types",  "");

        dstIndex.setIndexProperty("index.document.class",            "it.cnr.isti.hpclab.ef.structures.EFDocumentIndex");
        dstIndex.setIndexProperty("index.document.parameter_types",  "org.terrier.structures.IndexOnDisk");
        dstIndex.setIndexProperty("index.document.parameter_values", "index");

        dstIndex.setIndexProperty("index.document-inputstream.class",            "it.cnr.isti.hpclab.ef.structures.EFDocumentIndex$InputIterator");
        dstIndex.setIndexProperty("index.document-inputstream.parameter_types",  "org.terrier.structures.IndexOnDisk");
        dstIndex.setIndexProperty("index.document-inputstream.parameter_values", "index");
        
        dstIndex.setIndexProperty("index.inverted.class",               "it.cnr.isti.hpclab.ef.structures.EFInvertedIndex");
        dstIndex.setIndexProperty("index.inverted.parameter_types",  "org.terrier.structures.IndexOnDisk,org.terrier.structures.DocumentIndex");
        dstIndex.setIndexProperty("index.inverted.parameter_values", "index,document");
        
        dstIndex.setIndexProperty("index.inverted-inputstream.class",               "it.cnr.isti.hpclab.ef.structures.EFInvertedIndex$InputIterator");
        dstIndex.setIndexProperty("index.inverted-inputstream.parameter_types",  "org.terrier.structures.IndexOnDisk");
        dstIndex.setIndexProperty("index.inverted-inputstream.parameter_values", "index");
        
        if (withPos) {
            dstIndex.setIndexProperty(EliasFano.HAS_POSITIONS, "true");
        }
        dstIndex.flush();
    }
    
    public Generator(final IndexRef srcRef, final IndexRef dstRef) throws Exception 
    {    
        // Load input index
    	IndexOnDisk srcIndex = (IndexOnDisk) IndexFactory.of(srcRef);
    	
        if (IndexOnDisk.getLastIndexLoadError() != null) {
            throw new IllegalArgumentException("Error loading index: " + IndexOnDisk.getLastIndexLoadError());
        }
        this.numTerms = srcIndex.getCollectionStatistics().getNumberOfUniqueTerms();
        srcIndex.close();
        LOGGER.info("Input index contains " + this.numTerms + " terms");
        
        // check dst index does not exist
        final String dstIndexPath = FilenameUtils.getFullPath(dstRef.toString());
        
        if (!Files.exists(Paths.get(dstIndexPath))) {
            LOGGER.info("Index directory " + dstIndexPath + " does not exist. It is being created.");
            Files.createDirectories(Paths.get(dstIndexPath));
        } else if (Files.exists(Paths.get(dstRef.toString()))) {
            throw new IllegalArgumentException("Index directory " + dstIndexPath + " already contains an index with the given prefix");
        }        
    }

    public TermPartition[] partition(final int numThreads)
    {
        return TermPartition.split(numTerms, numThreads);
    }
}