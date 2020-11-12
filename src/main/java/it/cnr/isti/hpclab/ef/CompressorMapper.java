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
import java.util.function.Function;

import org.terrier.querying.IndexRef;

import org.terrier.structures.IndexFactory;
import org.terrier.structures.IndexOnDisk;

class CompressorMapper implements Function<TermPartition,TermPartition>
{
	private final IndexRef srcRef;
	private final IndexRef dstRef;
    private final boolean withPos;

    public CompressorMapper(final IndexRef srcRef, final IndexRef dstRef, final boolean withPos) 
    {
        this.srcRef = srcRef;
        this.dstRef = dstRef;

        this.withPos = withPos;
    }

    @Override
    public TermPartition apply(TermPartition terms) 
    {
        terms.prefix("_partition_" + terms.id());
        
        IndexOnDisk index = (IndexOnDisk) IndexFactory.of(srcRef);
        
        Compressor bc = (!withPos) 
            ? new BasicCompressor(index, dstRef)
            : new BlockCompressor(index, dstRef);
            
        try {
            bc.compress(terms);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return terms;
    }
}