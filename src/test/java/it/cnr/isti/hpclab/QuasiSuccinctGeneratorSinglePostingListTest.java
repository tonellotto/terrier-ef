package it.cnr.isti.hpclab;

import it.cnr.isti.hpclab.succinct.util.DocidReader;
import it.cnr.isti.hpclab.succinct.util.LongWordBitReader;
import it.cnr.isti.hpclab.succinct.util.LongWordBitWriter;
import it.cnr.isti.hpclab.succinct.util.SequenceEncoder;
import it.cnr.isti.hpclab.succinct.util.Utils;
import it.unimi.dsi.fastutil.longs.LongBigList;
import it.unimi.dsi.util.ByteBufferLongBigList;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel.MapMode;

import org.terrier.structures.Index;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;

public class QuasiSuccinctGeneratorSinglePostingListTest 
{
	@SuppressWarnings("resource")
	public static void main(final String args[]) throws IOException 
	{
		String path = "/Users/khast/index-java";
		//String dstPath = System.getProperty("ana.dir");
		//String dstPrefix = "cacca";

		Index srcIndex;

		srcIndex = Index.createIndex(path, "wt10g");
		if (Index.getLastIndexLoadError() != null) {
			System.err.println(Index.getLastIndexLoadError());
			System.exit(-2);
		}
		
		int numberOfDocuments = 1692096;
		int log2Quantum = 1;
		ByteOrder byteOrder = ByteOrder.nativeOrder();
				
		SequenceEncoder docidsAccumulator = new SequenceEncoder( 64 * 1024, log2Quantum );
		LongWordBitWriter docids = new LongWordBitWriter( new FileOutputStream("cacca.docids").getChannel(), byteOrder );

		LexiconEntry le;
		IterablePosting posting;
		
		le = srcIndex.getLexicon().getLexiconEntry("attori");
		posting = srcIndex.getInvertedIndex().getPostings(le);
		
		long lastDocid;
		long writtenBits;
		
		lastDocid = 0;
		docidsAccumulator.init( le.getDocumentFrequency(), numberOfDocuments, false, true, log2Quantum );
		
		System.err.println("Writing " + le.getDocumentFrequency() + " docids");
		while (posting.next() != IterablePosting.END_OF_LIST) {
			System.err.println(posting);
 
			docidsAccumulator.add( posting.getId() - lastDocid );
			lastDocid = posting.getId();
		}
		
		writtenBits = docidsAccumulator.dump(docids);
		System.err.println("Written " + writtenBits + " bits");
		
		
		long beginOfSecondPostingList = writtenBits;
		
		
		le = srcIndex.getLexicon().getLexiconEntry("attorno");
		posting = srcIndex.getInvertedIndex().getPostings(le);
		
		lastDocid = 0;
		docidsAccumulator.init( le.getDocumentFrequency(), numberOfDocuments, false, true, log2Quantum );
		
		System.err.println("Writing " + le.getDocumentFrequency() + " docids");
		while (posting.next() != IterablePosting.END_OF_LIST) {
			System.err.println(posting);
 
			docidsAccumulator.add( posting.getId() - lastDocid );
			lastDocid = posting.getId();
		}
		
		writtenBits = docidsAccumulator.dump(docids);
		System.err.println("Written " + writtenBits + " bits");
		
		docidsAccumulator.close();
		docids.close();

		

		
		
		
		le = srcIndex.getLexicon().getLexiconEntry("attori");
		LongBigList docidsList = ByteBufferLongBigList.map( new FileInputStream( "cacca.docids" ).getChannel(), byteOrder, MapMode.READ_ONLY );
		LongWordBitReader docidsLongWordBitReader = new LongWordBitReader( docidsList, 0 );
		docidsLongWordBitReader.position(0);
		
		// the number of lower bits for the EF encoding of a list of given length, upper bound and strictness.
		int l = Utils.lowerBits( le.getDocumentFrequency() + 1, numberOfDocuments, false );
		// the size in bits of forward or skip pointers to the EF encoding of a list of given length, upper bound and strictness.
		int pointerSize = Utils.pointerSize( le.getDocumentFrequency() + 1, numberOfDocuments, false, true );
		// the number of skip pointers to the EF encoding of a list of given length, upper bound and strictness.
		long numberOfPointers = Utils.numberOfPointers( le.getDocumentFrequency() + 1, numberOfDocuments, log2Quantum, false, true );

		// Reader of elements of size pointerSize
		LongWordBitReader skipPointers = new LongWordBitReader( docidsList, pointerSize );
		// Reader of elements of size l
		LongWordBitReader lowerBits = new LongWordBitReader( docidsList, l );

		// Where to start reading the skip pointers
		long skipPointersStart = docidsLongWordBitReader.position();
		// Where to start reading the lower bits array
		long lowerBitsStart = skipPointersStart + pointerSize * numberOfPointers;
		lowerBits.position( lowerBitsStart ); 						
		
		DocidReader efpr = new DocidReader( docidsList, lowerBits, lowerBitsStart, l, skipPointers, skipPointersStart, numberOfPointers, pointerSize, le.getDocumentFrequency(), log2Quantum );
		
		//System.err.println(efpr.skipTo(1397258));
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());

		
		
		
		
		
		
		
		
		
		
		
		
		
		
		le = srcIndex.getLexicon().getLexiconEntry("attorno");
		docidsList = ByteBufferLongBigList.map( new FileInputStream( "cacca.docids" ).getChannel(), byteOrder, MapMode.READ_ONLY );
		docidsLongWordBitReader = new LongWordBitReader( docidsList, 0 );
		docidsLongWordBitReader.position(beginOfSecondPostingList);
		
		// the number of lower bits for the EF encoding of a list of given length, upper bound and strictness.
		l = Utils.lowerBits( le.getDocumentFrequency() + 1, numberOfDocuments, false );
		// the size in bits of forward or skip pointers to the EF encoding of a list of given length, upper bound and strictness.
		pointerSize = Utils.pointerSize( le.getDocumentFrequency() + 1, numberOfDocuments, false, true );
		// the number of skip pointers to the EF encoding of a list of given length, upper bound and strictness.
		numberOfPointers = Utils.numberOfPointers( le.getDocumentFrequency() + 1, numberOfDocuments, log2Quantum, false, true );

		// Reader of elements of size pointerSize
		skipPointers = new LongWordBitReader( docidsList, pointerSize );
		// Reader of elements of size l
		lowerBits = new LongWordBitReader( docidsList, l );

		// Where to start reading the skip pointers
		skipPointersStart = docidsLongWordBitReader.position();
		// Where to start reading the lower bits array
		lowerBitsStart = skipPointersStart + pointerSize * numberOfPointers;
		lowerBits.position( lowerBitsStart ); 						
		
		efpr = new DocidReader( docidsList, lowerBits, lowerBitsStart, l, skipPointers, skipPointersStart, numberOfPointers, pointerSize, le.getDocumentFrequency(), log2Quantum );
		
		//System.err.println(efpr.skipTo(1397258));
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
		System.err.println(efpr.getNextPrefixSum());
	}	
}
