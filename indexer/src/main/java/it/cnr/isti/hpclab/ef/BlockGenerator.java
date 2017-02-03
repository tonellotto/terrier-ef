package it.cnr.isti.hpclab.ef;

import it.cnr.isti.hpclab.ef.structures.EFDocumentIndex;
import it.cnr.isti.hpclab.ef.structures.EFLexiconEntry;
import it.cnr.isti.hpclab.ef.structures.EFBlockIterablePosting;
import it.cnr.isti.hpclab.ef.structures.EFBlockLexiconEntry;
import it.cnr.isti.hpclab.ef.util.IndexUtil;
import it.cnr.isti.hpclab.ef.util.LongWordBitWriter;
import it.cnr.isti.hpclab.ef.util.SequenceEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.terrier.structures.BasicLexiconEntry;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.FSOMapFileLexiconOutputStream;
import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.LexiconOutputStream;
import org.terrier.structures.collections.FSOrderedMapFile;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.seralization.FixedSizeTextFactory;

import org.terrier.utility.TerrierTimer;

public class BlockGenerator 
{
	public static int LOG2QUANTUM = Integer.parseInt(System.getProperty(EliasFano.LOG2QUANTUM, "8"));
	private static ByteOrder BYTEORDER = ByteOrder.nativeOrder();
	private static int DEFAULT_CACHE_SIZE = 64 * 1024 * 1024;
	
	public static void link(String from, String to) throws IOException
	{
		Runtime.getRuntime().exec("ln -s " + from + " " + to);  
	}
	
	public static void main(final String args[]) throws IOException 
	{
		Index.setIndexLoadingProfileAsRetrieval(false);
		
		if (args.length != 2) {
			System.err.println("Usage: java " + BlockGenerator.class.getName() + " <index.path> <src.index.prefix>");
			System.exit(-1);
		}

		String path = args[0];
		String srcPrefix = args[1];
		String dstPrefix = srcPrefix + EliasFano.USUAL_EXTENSION;

		IndexOnDisk srcIndex;

		srcIndex = Index.createIndex(path, srcPrefix);
		if (Index.getLastIndexLoadError() != null) {
			System.err.println(Index.getLastIndexLoadError());
			System.exit(-2);
		}
				
		createLexiconDocidsFreqsPos(path, dstPrefix, srcIndex);
		createDocumentIndex(path, dstPrefix, srcIndex);
		// createMetaIndex(path, dstPrefix, srcIndex);
		createProperties(path, dstPrefix, srcIndex);
		
		IndexOnDisk dstIndex;
		dstIndex = Index.createIndex(path, dstPrefix);
		if (Index.getLastIndexLoadError() != null) {
			System.err.println(Index.getLastIndexLoadError());
			System.exit(-2);
		}
				
		randomSanityCheck(srcIndex, dstIndex);
	}

	public static void randomSanityCheck(IndexOnDisk srcIndex, IndexOnDisk dstIndex) throws IOException 
	{
		Random rnd = new Random(System.currentTimeMillis());
		int numSamples = 50 + 1 + rnd.nextInt(50);
		System.err.println("Randomly checking " + numSamples + " terms and posting list for sanity check");
		
		if (srcIndex.getCollectionStatistics().getNumberOfUniqueTerms() != dstIndex.getCollectionStatistics().getNumberOfUniqueTerms()) {
			System.err.println("Original  index has " + srcIndex.getCollectionStatistics().getNumberOfUniqueTerms() + " unique terms");
			System.err.println("EliasFano index has " + dstIndex.getCollectionStatistics().getNumberOfUniqueTerms() + " unique terms");
			System.exit(-1);
		}

		Map.Entry<String, LexiconEntry> originalEntry;
		Map.Entry<String, LexiconEntry> efEntry;
		
		BasicLexiconEntry ble;
		EFLexiconEntry efle;

		for (int i = 0; i < numSamples; i++) {
			int termid = rnd.nextInt(dstIndex.getCollectionStatistics().getNumberOfUniqueTerms());
			
			originalEntry = srcIndex.getLexicon().getLexiconEntry(termid);
			efEntry       = dstIndex.getLexicon().getLexiconEntry(termid);
						
			ble  = (BasicLexiconEntry) originalEntry.getValue();
			efle = (EFLexiconEntry) efEntry.getValue();

			System.err.println("Checking term " + originalEntry.getKey() + " (" + originalEntry.getValue().getDocumentFrequency() + " entries), termid " + termid + " Tot pos " + originalEntry.getValue().getFrequency());

			IterablePosting srcPosting = srcIndex.getInvertedIndex().getPostings(ble);
			EFBlockIterablePosting dstPosting = (EFBlockIterablePosting) dstIndex.getInvertedIndex().getPostings(efle);
						
			while (srcPosting.next() != IterablePosting.END_OF_LIST && dstPosting.next() != IterablePosting.END_OF_LIST) {
				if ((srcPosting.getId() != dstPosting.getId()) || (srcPosting.getFrequency() != dstPosting.getFrequency())) {
					System.err.println("Something went wrong in random sanity check...");
					System.exit(-1);
				}
				int[] srcPositions = ((BlockPosting)srcPosting).getPositions();
				int[] dstPositions = dstPosting.getPositions();
				
				if (srcPositions.length != dstPositions.length) {
					System.err.println("Something went wrong in random sanity check...");
					System.exit(-1);
				}

				for (int j = 0; j < srcPositions.length; ++j) {
//					System.err.println("src " + srcPositions[j] + "\tdst " + dstPositions[j]);
					if (srcPositions[j] != dstPositions[j]) {
						System.err.println("Something went wrong in random sanity check...");
						System.exit(-1);
					}
				}
			}
		}
	}

	private static void createProperties(final String path, final String dstPrefix, final IndexOnDisk srcIndex) throws IOException 
	{		
		String filename = path + File.separator + dstPrefix + ".properties";
		int num_docs = srcIndex.getCollectionStatistics().getNumberOfDocuments();
		int num_terms = srcIndex.getCollectionStatistics().getNumberOfUniqueTerms();
		long num_pointers = srcIndex.getCollectionStatistics().getNumberOfPointers();
		long num_tokens = srcIndex.getCollectionStatistics().getNumberOfTokens();
		
		IndexUtil.writeEFPosIndexProperties(filename, num_docs, num_terms, num_pointers, num_tokens, LOG2QUANTUM);
  	}

	private static void createDocumentIndex(final String path, final String dstPrefix, final IndexOnDisk srcIndex) throws IOException 
	{
		EFDocumentIndex.write((org.terrier.structures.DocumentIndex) srcIndex.getDocumentIndex(), path + File.separator + dstPrefix + ".sizes");
	}

	/*
	private static void createMetaIndex(final String path, final String dstPrefix, final IndexOnDisk srcIndex) throws IOException 
	{
		Files.copyFile(srcIndex.getPath() + ApplicationSetup.FILE_SEPARATOR + srcIndex.getPrefix() + ".meta.zdata", path + ApplicationSetup.FILE_SEPARATOR + dstPrefix + ".meta.zdata");
		Files.copyFile(srcIndex.getPath() + ApplicationSetup.FILE_SEPARATOR + srcIndex.getPrefix() + ".meta.idx", path + ApplicationSetup.FILE_SEPARATOR + dstPrefix + ".meta.idx");
	}
	*/

	@SuppressWarnings("resource")
	private static void createLexiconDocidsFreqsPos(final String path, final String dstPrefix, final IndexOnDisk srcIndex) throws IOException 
	{
		// The new lexicon writer (please note it is an output stream)
		LexiconOutputStream<String> los = new FSOMapFileLexiconOutputStream(path + File.separator + dstPrefix + ".lexicon" + FSOrderedMapFile.USUAL_EXTENSION, new FixedSizeTextFactory(IndexUtil.DEFAULT_MAX_TERM_LENGTH));
				
		int numberOfDocuments = srcIndex.getCollectionStatistics().getNumberOfDocuments();
		// The sequence encoder to generate posting lists (docids)
		SequenceEncoder docidsAccumulator = new SequenceEncoder( DEFAULT_CACHE_SIZE, LOG2QUANTUM );
		// The sequence encoder to generate posting lists (freqs)
		SequenceEncoder freqsAccumulator = new SequenceEncoder( DEFAULT_CACHE_SIZE, LOG2QUANTUM );
		// The sequence encoder to generate posting lists (positions)
		SequenceEncoder posAccumulator = new SequenceEncoder( DEFAULT_CACHE_SIZE, LOG2QUANTUM );
		
		// The new docids inverted file
		LongWordBitWriter docids = new LongWordBitWriter( new FileOutputStream(path + File.separator + dstPrefix + EliasFano.DOCID_EXTENSION).getChannel(), BYTEORDER );
		// The new freqs inverted file
		LongWordBitWriter freqs = new LongWordBitWriter( new FileOutputStream(path + File.separator + dstPrefix + EliasFano.FREQ_EXTENSION).getChannel(), BYTEORDER );
		// The new positions inverted file
		LongWordBitWriter pos = new LongWordBitWriter( new FileOutputStream(path + File.separator + dstPrefix + EliasFano.POS_EXTENSION).getChannel(), BYTEORDER );

		TerrierTimer tt = new TerrierTimer("Creating EliasFano Lexicon, Docids and Frequencies", srcIndex.getCollectionStatistics().getNumberOfUniqueTerms());tt.start();    
		Iterator<Entry<String, LexiconEntry>> lexiconIterator = srcIndex.getLexicon().iterator();
				
		long docidsOffset = 0;
		long freqsOffset = 0;
		long posOffset = 0;
		
		int cnt = 0;
		while (lexiconIterator.hasNext()) {
			// We get the next lexicon entry from the source index (assuming them ordered by termid)
			final Map.Entry<String, LexiconEntry> leIn = lexiconIterator.next();
			final LexiconEntry le = leIn.getValue();
/*
			if (le.getTermId() == 951)
				System.err.println("CAZZO");
*/
			IterablePosting p = srcIndex.getInvertedIndex().getPostings((BitIndexPointer)le);
					 			
			docidsAccumulator.init( le.getDocumentFrequency(), numberOfDocuments, false, true, LOG2QUANTUM );
			freqsAccumulator.init(  le.getDocumentFrequency(), le.getFrequency(), true, false, LOG2QUANTUM );

			long sumMaxPos = 0; // in the first pass, we need to compute the upper bound to encode positions
			
			long lastDocid = 0;
			long occurrency = 0; // Do not trust le.getFrequency() because of block max limit!
			while (p.next() != IterablePosting.END_OF_LIST) {
				docidsAccumulator.add( p.getId() - lastDocid );
				lastDocid = p.getId();
				freqsAccumulator.add(p.getFrequency());
				sumMaxPos += ((BlockPosting)p).getPositions()[((BlockPosting)p).getPositions().length - 1];
				occurrency += ((BlockPosting)p).getPositions().length;
			}
			p.close();
			
			if (occurrency != le.getFrequency())
					throw new IllegalStateException("Lexicon term occurencies (" + le.getFrequency() + ") different form positions-counted occurrencies (" + occurrency + ")");
/*
			if (occurrency < le.getFrequency()) {
				System.err.println("Lexicon term occurencies (" + le.getFrequency() + ") more than form positions-counted occurrencies (" + occurrency + ")");
				System.err.println("Max length of positions array was enforced, please remember to set \"blocks.max\"  in future Terrier indexing processes");
				System.err.println("Using le.getFrequency() as positions UB in EF, with no guarantees");
			}
*/
			// We create the new lexicon entry with skip offset data included
			EFBlockLexiconEntry leOut = new EFBlockLexiconEntry(le.getTermId(), le.getDocumentFrequency(), le.getFrequency(), docidsOffset, freqsOffset, posOffset, sumMaxPos);
			// We write the new lexicon entry to the new lexicon
			los.writeNextEntry(leIn.getKey(), leOut);
			
			// After computing sumMaxPos, we re-scan the posting list to encode the positions
			posAccumulator.init(le.getFrequency(), le.getDocumentFrequency() + sumMaxPos, true, false, LOG2QUANTUM );
			
			p = srcIndex.getInvertedIndex().getPostings((BitIndexPointer)le);
			
			int[] positions = null;
			while (p.next() != IterablePosting.END_OF_LIST) {
				positions = ((BlockPosting)p).getPositions();
				posAccumulator.add(1 + positions[0]);
				for (int i = 1; i < positions.length; i++)
					posAccumulator.add(positions[i] - positions[i-1]);
			}
			p.close();
			
			docidsOffset += docidsAccumulator.dump(docids);		
			freqsOffset  += freqsAccumulator.dump(freqs);
			
			// Firstly we write decoding limits info
			posOffset += pos.writeGamma(posAccumulator.lowerBits());
			posOffset += posAccumulator.numberOfPointers() == 0 ? 0 : pos.writeNonZeroGamma( posAccumulator.pointerSize() );
			// Secondly we dump the EF representation of the position encoding
			posOffset += posAccumulator.dump(pos);
			
			tt.increment();
			cnt++;
		}
		tt.finished();
		System.err.println("Total " + srcIndex.getCollectionStatistics().getNumberOfUniqueTerms() + ", processed " + cnt);
				
		docidsAccumulator.close();
		docids.close();
		
		freqsAccumulator.close();
		freqs.close();
		
		posAccumulator.close();
		pos.close();
		
		los.close();
	}
}