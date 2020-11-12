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

import org.apache.commons.io.FilenameUtils;

import org.terrier.querying.IndexRef;

import org.terrier.structures.FSOMapFileLexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.collections.FSOrderedMapFile;
import org.terrier.structures.seralization.FixedSizeTextFactory;

import it.cnr.isti.hpclab.ef.util.IndexUtil;
import it.cnr.isti.hpclab.ef.structures.EFBlockLexiconEntry;
import it.cnr.isti.hpclab.ef.structures.EFLexiconEntry;
import it.cnr.isti.hpclab.ef.structures.FSOMapFileAppendLexiconOutputStream;

public class CompressorReducer implements BinaryOperator<TermPartition> 
{
    private final IndexRef dstRef;
    private final boolean withPos;
    
    public CompressorReducer(final IndexRef dstRef, final boolean withPos)
    {
        this.dstRef = dstRef;
        this.withPos = withPos;
    }

    @Override
    public TermPartition apply(TermPartition t1, TermPartition t2) 
    {
        final String outPrefix = "_merge_" + t1.id();
        final String dstIndexPath = FilenameUtils.getFullPath(dstRef.toString());
        
        try {
            // Merge docids (low level)
            long docidOffset = merge(t1.prefix() + EliasFano.DOCID_EXTENSION, 
                                      t2.prefix() + EliasFano.DOCID_EXTENSION, 
                                      outPrefix  + EliasFano.DOCID_EXTENSION);

            // Merge frequencies (low level)
            long freqOffset = merge(t1.prefix() + EliasFano.FREQ_EXTENSION,
                                     t2.prefix() + EliasFano.FREQ_EXTENSION, 
                                     outPrefix  + EliasFano.FREQ_EXTENSION);

            long posOffset = (withPos) 
                ? merge(t1.prefix() + EliasFano.POS_EXTENSION,
                        t2.prefix() + EliasFano.POS_EXTENSION, 
                        outPrefix  + EliasFano.POS_EXTENSION)
                : 0;

            // Merge lexicons (inplace t1 merge with t2 while recomputing offsets)
            FSOMapFileAppendLexiconOutputStream los1 = 
            		new FSOMapFileAppendLexiconOutputStream(dstIndexPath + File.separator + t1.prefix() + ".lexicon" + FSOrderedMapFile.USUAL_EXTENSION,
                                                            new FixedSizeTextFactory(IndexUtil.DEFAULT_MAX_TERM_LENGTH),
                                                            (!withPos) ? new EFLexiconEntry.Factory() : new EFBlockLexiconEntry.Factory());

            Iterator<Entry<String, LexiconEntry>> lexIter = null; 
            Entry<String, LexiconEntry> lee = null;
            FSOMapFileLexicon lex = null;

            lex = new FSOMapFileLexicon("lexicon", dstIndexPath, t2.prefix(),
                                        new FixedSizeTextFactory(IndexUtil.DEFAULT_MAX_TERM_LENGTH),
                                        (!withPos) ? new EFLexiconEntry.Factory() : new EFBlockLexiconEntry.Factory(),
                                        "aligned", "default", "file");
            lexIter = lex.iterator();

            while (lexIter.hasNext()) {
                lee = lexIter.next();
                if (withPos) {
                    EFBlockLexiconEntry le = (EFBlockLexiconEntry) lee.getValue();
                    le.docidOffset += Byte.SIZE * docidOffset;
                    le.freqOffset  += Byte.SIZE * freqOffset;
                    le.posOffset   += Byte.SIZE * posOffset;
                    // le.termId += num_terms_1;
                    los1.writeNextEntry(lee.getKey(), le);
                } else {
                    EFLexiconEntry le = (EFLexiconEntry) lee.getValue();
                    le.docidOffset += Byte.SIZE * docidOffset;
                    le.freqOffset  += Byte.SIZE * freqOffset;
                    // le.termId += num_terms_1;
                    los1.writeNextEntry(lee.getKey(), le);
                }
            }

            lex.close();
            los1.close();

            Files.move(Paths.get(dstIndexPath, t1.prefix() + ".lexicon" + FSOrderedMapFile.USUAL_EXTENSION),
                       Paths.get(dstIndexPath, outPrefix  + ".lexicon" + FSOrderedMapFile.USUAL_EXTENSION));
            Files.delete(Paths.get(dstIndexPath, t2.prefix() + ".lexicon" + FSOrderedMapFile.USUAL_EXTENSION));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Set correct prefix for next merging and return it
        t1.prefix(outPrefix);
        t1.id(t2.id());
        return t1;
    }

    private long merge(final String prefixIn1, final String prefixIn2, final String outPrefix) throws IOException 
    {
        final String dstIndexPath = FilenameUtils.getFullPath(dstRef.toString());
    	
        Path inFile1 = Paths.get(dstIndexPath + File.separator + prefixIn1);
        Path inFile2 = Paths.get(dstIndexPath + File.separator + prefixIn2);
        Path outFile  = Paths.get(dstIndexPath + File.separator + outPrefix);

        final long offset = Files.size(inFile1);
        try (FileChannel out = FileChannel.open(inFile1, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            try (FileChannel in = FileChannel.open(inFile2, StandardOpenOption.READ)) {
                long l = in.size();
                for (long p = 0; p < l;)
                    p += in.transferTo(p, l - p, out);
            }
        }

        Files.move(inFile1, outFile);
        Files.delete(inFile2);
        return offset;
    }
}