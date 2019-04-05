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
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.BinaryOperator;

import org.terrier.structures.FSOMapFileLexicon;
import org.terrier.structures.Index;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.collections.FSOrderedMapFile;
import org.terrier.structures.seralization.FixedSizeTextFactory;

import it.cnr.isti.hpclab.ef.util.IndexUtil;
import it.cnr.isti.hpclab.ef.structures.EFBlockLexiconEntry;
import it.cnr.isti.hpclab.ef.structures.EFLexiconEntry;
import it.cnr.isti.hpclab.ef.structures.FSOMapFileAppendLexiconOutputStream;

public class CompressorReducer implements BinaryOperator<TermPartition> 
{
	private final String dst_index_path;
	private final String dst_index_prefix;
	private final boolean with_pos;
	
	public CompressorReducer(final String dst_index_path, final String dst_index_prefix, final boolean with_pos)
	{
		this.dst_index_path = dst_index_path;
		this.dst_index_prefix = dst_index_prefix;
		this.with_pos = with_pos;
	}

	@Override
	public TermPartition apply(TermPartition t1, TermPartition t2) 
	{
		Index.setIndexLoadingProfileAsRetrieval(false);
		String out_prefix = this.dst_index_prefix + "_merge_" + t1.id();
		
		try {
			// Merge docids (low level)
			long docid_offset = merge(t1.prefix() + EliasFano.DOCID_EXTENSION, 
									  t2.prefix() + EliasFano.DOCID_EXTENSION, 
									  out_prefix  + EliasFano.DOCID_EXTENSION);
		
			// Merge frequencies (low level)
			long freq_offset = merge(t1.prefix() + EliasFano.FREQ_EXTENSION,
									 t2.prefix() + EliasFano.FREQ_EXTENSION, 
									 out_prefix  + EliasFano.FREQ_EXTENSION);

			long pos_offset = (with_pos) 
				? merge(t1.prefix() + EliasFano.POS_EXTENSION,
						t2.prefix() + EliasFano.POS_EXTENSION, 
						out_prefix  + EliasFano.POS_EXTENSION)
				: 0;

			// Merge lexicons (inplace t1 merge with t2 while recomputing offsets)
			FSOMapFileAppendLexiconOutputStream los1 = new FSOMapFileAppendLexiconOutputStream(this.dst_index_path + File.separator + t1.prefix() + ".lexicon" + FSOrderedMapFile.USUAL_EXTENSION,
											  							   			   		   new FixedSizeTextFactory(IndexUtil.DEFAULT_MAX_TERM_LENGTH),
											  							   			   		   (!with_pos) ? new EFLexiconEntry.Factory() : new EFBlockLexiconEntry.Factory());
			final int num_terms_1 = (int) (Files.size(Paths.get(dst_index_path + File.separator + t1.prefix() + ".lexicon" + FSOrderedMapFile.USUAL_EXTENSION)) / los1.getEntrySize());
			
			Iterator<Entry<String, LexiconEntry>> lex_iter = null; 
			Entry<String, LexiconEntry> lee = null;
			FSOMapFileLexicon lex = null;
			
//			Files.delete(Paths.get(dst_index_path, t1.prefix() + ".lexicon" + FSOrderedMapFile.USUAL_EXTENSION));
			
			lex = new FSOMapFileLexicon("lexicon", dst_index_path, t2.prefix(), 
					new FixedSizeTextFactory(IndexUtil.DEFAULT_MAX_TERM_LENGTH),
		    		(!with_pos) ? new EFLexiconEntry.Factory() : new EFBlockLexiconEntry.Factory(),
		    		"aligned", "default", "file");
			lex_iter = lex.iterator();
			
			while (lex_iter.hasNext()) {
				lee = lex_iter.next();
				if (with_pos) {
					EFBlockLexiconEntry le = (EFBlockLexiconEntry) lee.getValue();
					le.docidOffset += Byte.SIZE * docid_offset;
					le.freqOffset  += Byte.SIZE * freq_offset;
					le.posOffset   += Byte.SIZE * pos_offset;
					le.termId += num_terms_1;
					los1.writeNextEntry(lee.getKey(), le);
				} else {
					EFLexiconEntry le = (EFLexiconEntry) lee.getValue();
					le.docidOffset += Byte.SIZE * docid_offset;
					le.freqOffset  += Byte.SIZE * freq_offset;
					le.termId += num_terms_1;
					los1.writeNextEntry(lee.getKey(), le);
				}
			}
			
			lex.close();
			los1.close();
			
			Files.move(Paths.get(this.dst_index_path, t1.prefix() + ".lexicon" + FSOrderedMapFile.USUAL_EXTENSION),
					   Paths.get(this.dst_index_path, out_prefix  + ".lexicon" + FSOrderedMapFile.USUAL_EXTENSION));
			Files.delete(Paths.get(dst_index_path, t2.prefix() + ".lexicon" + FSOrderedMapFile.USUAL_EXTENSION));
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Set correct prefix for next merging and return it
		t1.setPrefix(out_prefix);
		t1.setId(t2.id());
		return t1;
	}	
	
	private long merge(final String prefix_in1, final String prefix_in2, final String out_prefix) throws IOException 
	{
		Path in_file_1 = Paths.get(this.dst_index_path + File.separator + prefix_in1);
		Path in_file_2 = Paths.get(this.dst_index_path + File.separator + prefix_in2);
		Path out_file  = Paths.get(this.dst_index_path + File.separator + out_prefix);

		final long offset = Files.size(in_file_1);
	    try (FileChannel out = FileChannel.open(in_file_1, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
	        try (FileChannel in = FileChannel.open(in_file_2, StandardOpenOption.READ)) {
	    	    for (long p = 0, l = in.size(); p < l; )
	    	    	p += in.transferTo(p, l - p, out);
	        }
	    }
	    
		Files.move(in_file_1, out_file);
	    Files.delete(in_file_2);
	    return offset;
	}
};