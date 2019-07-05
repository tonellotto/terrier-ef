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

import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.Iterator;

import org.apache.commons.io.FilenameUtils;

import org.apache.log4j.Logger;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.PostingIndexInputStream;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.UnitUtils;
import org.terrier.structures.DocumentIndexEntry;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import it.cnr.isti.hpclab.ef.structures.EFDirectIndex;

public class Invert2Direct 
{
	private static Logger LOGGER = Logger.getLogger( Invert2Direct.class );
	
	public static final class Posting
	{
		public final int docid;
		public final int tf;
		
		public Posting(final int docid, final int tf)
		{
			this.docid = docid;
			this.tf = tf;
		}
	}
	
	public static final class Args 
	{
	    // required arguments

	    @Option(name = "-index",  metaVar = "[String]", required = true, usage = "Input Index")
	    public String index;

	    // optional arguments
	    /*
	    @Option(name = "-p", metaVar = "[Number]", required = false, usage = "Parallelism degree")
	    public String parallelism;
	    
	    @Option(name = "-b", required = false, usage = "Compress positions with Elias-Fano")
	    public boolean with_pos = false;
		*/
	}
	
	public static void main(String[] argv) throws IOException
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

	private static void process(Args args) throws IOException 
	{
		IndexOnDisk.setIndexLoadingProfileAsRetrieval(false);
				
		final String src_index_path = FilenameUtils.getFullPath(args.index);
		final String src_index_prefix = FilenameUtils.getBaseName(args.index);

		IndexOnDisk index = Index.createIndex(src_index_path, src_index_prefix);
		
		if (!index.hasIndexStructure("inverted")) {
			LOGGER.error("This index has no inverted structure, aborting direct index build");
			return;
		}
		
		if ( index.hasIndexStructure("direct")) {
			LOGGER.error("This index already has a direct index, no need to create one.");
			return;
		}
		
		if ( !"aligned".equals(index.getIndexProperty("index.lexicon.termids", "")))
		{
			LOGGER.error("This index is not supported by " + Invert2Direct.class.getName() + " - termids are not strictly ascending.");
			return;
		}
		
		LOGGER.info("Generating a direct structure from the inverted structure");
		
		/** number of tokens limit per iteration */
		final long processTokens = UnitUtils.parseLong(ApplicationSetup.getProperty("inverted2direct.processtokens", "100000000"));
		
		/** total tokens to process */
		final long totalTokens = index.getCollectionStatistics().getNumberOfTokens();
		
		/** total iterations to perform */
		final int totalIterations = (int)((totalTokens % processTokens == 0) ? (totalTokens/processTokens) : (totalTokens/processTokens + 1));
		
		/** info string */
		// 	final String iterationSuffix = (processTokens > totalTokens) ? " of 1 iteration" : " of " + totalIterations + " iterations";
		
		int firstDocid = 0;
		long numberOfTokensFound = 0;	

		@SuppressWarnings("unchecked")
		Iterator<DocumentIndexEntry> diis =  (Iterator<DocumentIndexEntry>) index.getIndexStructureInputStream("document");
		
		DirectIndexWriter diw = new DirectIndexWriter(index);
		
		ProgressBarBuilder pbBuilder = new ProgressBarBuilder()
			    .setInitialMax(totalIterations)
			    .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
			    .setTaskName("iterations")
			    .setUpdateIntervalMillis(1000)
			    .showSpeed(new DecimalFormat("#.###"));

		try (ProgressBar pb = pbBuilder.build()) { 
			do {
				// get a copy of the inverted index input iterator
				final PostingIndexInputStream iiis = (PostingIndexInputStream) index.getIndexStructureInputStream("inverted");
			
				// work out how many document we can scan for
				int countDocsThisIteration = scanDocumentIndexForTokens(processTokens, diis); 
			
				// get a set of posting objects to save the compressed postings for each of the documents to
				final ObjectList<Posting>[] postings = createPostings(countDocsThisIteration);
			
				// get postings for these documents
				numberOfTokensFound += traverseInvertedFile(iiis, firstDocid, countDocsThisIteration, postings);
			
				diw.dump(postings, firstDocid);
			
				iiis.close();
				firstDocid = firstDocid + countDocsThisIteration;
				pb.step();
			} while (firstDocid < index.getCollectionStatistics().getNumberOfDocuments());
		}
		
		assert firstDocid == index.getCollectionStatistics().getNumberOfDocuments() : " firstDocid=" + firstDocid;

		if (numberOfTokensFound != totalTokens)
			LOGGER.warn("Number of tokens found while scanning inverted structure does not match expected. Expected " + index.getCollectionStatistics().getNumberOfTokens()+ ", found " + numberOfTokensFound);
		
		writeProperties(index);
		
		diw.close();
		index.close();
	}
	
	/** 
	 * Iterates through the document index, until it has reached the given number of terms.
	 * 
	 * @param processTokens the number of tokens to stop reading the document index after
	 * @param docidStream the document index stream to read 
	 * @return the number of documents to process
	 */
	protected static int scanDocumentIndexForTokens(final long processTokens, final Iterator<DocumentIndexEntry> docidStream) throws IOException
	{
		long tokens = 0; 
		int docs = 0;
		while (docidStream.hasNext()) {
			docs++;
			tokens += docidStream.next().getDocumentLength();
			if (tokens >= processTokens)
				return docs;
		}
		return docs;
	}

	/** 
	 * Traverse the whole inverted file, term by term, looking for all occurrences of documents in the given range.
	 * 
	 * @return the number of tokens found in all of the documents
	 */
	protected static long traverseInvertedFile(final PostingIndexInputStream iiis, int firstDocid, int countDocuments, final ObjectList<Posting>[] directPostings) throws IOException
	{
		// Algorithm:
		// for each posting list in the inverted index
			// for each (in range) posting in list
				// add termid->tf tuple to the Posting array
		long numTokens = 0; 
		int  termId;
		
		final int lastDocid = firstDocid + countDocuments - 1;

		while (iiis.hasNext()) {
			
			IterablePosting ip = iiis.next();
			termId = ((LexiconEntry) iiis.getCurrentPointer()).getTermId();

			int docid = ip.next(firstDocid);
			while (docid <= lastDocid && docid != IterablePosting.EOL) {				
				numTokens += ip.getFrequency();
				directPostings[docid - firstDocid].add(new Posting(termId, ip.getFrequency()));
				docid = ip.next();
			}
		}
		return numTokens;
	}
	
	@SuppressWarnings("unchecked")
	protected static ObjectList<Posting>[] createPostings(final int count)
	{
		ObjectList<Posting>[] rtr = (ObjectList<Posting>[]) Array.newInstance(ObjectList.class, count);
		for (int i = 0; i < count; ++i)
			rtr[i] = new ObjectArrayList<Posting>();
		return rtr;
	}

	private static void writeProperties(final IndexOnDisk index) throws IOException 
	{
		index.addIndexStructure("direct", EFDirectIndex.class.getName(), "org.terrier.structures.IndexOnDisk", "index");
		/*
			index.addIndexStructureInputStream(
				destinationStructure, 
				directIndexInputStreamClass,
				"org.terrier.structures.IndexOnDisk,java.lang.String,java.util.Iterator,java.lang.Class",
				"index,structureName,document-inputstream,"+ 
					(fieldCount > 0 ? fieldDirectIndexPostingIteratorClass : basicDirectIndexPostingIteratorClass));
		*/
		index.flush(); //save changes
	}
}
