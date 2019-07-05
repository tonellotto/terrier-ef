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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import it.unimi.dsi.fastutil.objects.ObjectList;

import org.terrier.structures.DocumentIndex;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.collections.FSArrayFile;
import org.terrier.utility.Files;

import it.cnr.isti.hpclab.ef.Invert2Direct.Posting;
import it.cnr.isti.hpclab.ef.util.LongWordBitWriter;
import it.cnr.isti.hpclab.ef.util.SequenceEncoder;

public class DirectIndexWriter 
{
	protected static final int DEFAULT_CACHE_SIZE = 64 * 1024 * 1024;
	public static final int LOG2QUANTUM = 8;
	public static final int ENTRY_SIZE = Long.BYTES + Long.BYTES + Integer.BYTES;

	// writers
	protected final DataOutputStream  dos;			
	protected final LongWordBitWriter termids;
	protected final LongWordBitWriter freqs;

	// upper bounds to use
	protected final int upperBoundTermids;
	protected final DocumentIndex doi;

	// The sequence encoder to generate posting lists (termids)
	protected final SequenceEncoder termidsAccumulator = new SequenceEncoder( DEFAULT_CACHE_SIZE, LOG2QUANTUM );
	// The sequence encoder to generate posting lists (freqs)
	protected final SequenceEncoder freqsAccumulator   = new SequenceEncoder( DEFAULT_CACHE_SIZE, LOG2QUANTUM );

	protected long termidsOffset = 0;
	protected long freqsOffset = 0;


	@SuppressWarnings("resource")
	public DirectIndexWriter(final IndexOnDisk index) throws IOException
	{
	    dos     = new DataOutputStream(Files.writeFileStream(index.getPath() + File.separator + index.getPrefix() + ".direct" + FSArrayFile.USUAL_EXTENSION));			
		termids = new LongWordBitWriter(new FileOutputStream(index.getPath() + File.separator + index.getPrefix() + ".direct" + EliasFano.DOCID_EXTENSION).getChannel(), ByteOrder.nativeOrder());
		freqs   = new LongWordBitWriter(new FileOutputStream(index.getPath() + File.separator + index.getPrefix() + ".direct" + EliasFano.FREQ_EXTENSION).getChannel(),  ByteOrder.nativeOrder());
		
		this.upperBoundTermids = index.getCollectionStatistics().getNumberOfUniqueTerms();
		this.doi = index.getDocumentIndex();
	}
		
	public void dump(final ObjectList<Posting>[] postings, final int firstDocid) throws IOException
	{
		int docid = firstDocid;
		for (ObjectList<Posting> pl: postings) {
			dos.writeLong(termidsOffset);
			dos.writeLong(freqsOffset);
			dos.writeInt(pl.size());
			
			termidsAccumulator.init( pl.size(), upperBoundTermids,   false, true, LOG2QUANTUM );
			freqsAccumulator.init(   pl.size(), doi.getDocumentLength(docid++), true, false, LOG2QUANTUM );

			long lastTermid = 0;
			for (Posting p: pl) {
				termidsAccumulator.add(p.docid - lastTermid);
				lastTermid = p.docid;
				freqsAccumulator.add(p.tf);
			}
			
			termidsOffset += termidsAccumulator.dump(termids);		
			freqsOffset  += freqsAccumulator.dump(freqs);
		}
	}
	
	public void close() throws IOException
	{
		termidsAccumulator.close();
		freqsAccumulator.close();
		
		termids.close();
		freqs.close();
		
		dos.close();
	}
}
