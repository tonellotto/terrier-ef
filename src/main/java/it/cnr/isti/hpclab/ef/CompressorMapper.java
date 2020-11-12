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
	private final IndexRef src_ref, dst_ref;
    private final boolean with_pos;

    public CompressorMapper(final IndexRef src_ref, final IndexRef dst_ref, final boolean with_pos) 
    {
    	this.src_ref = src_ref;
    	this.dst_ref = dst_ref;

        this.with_pos = with_pos;
    }

    @Override
    public TermPartition apply(TermPartition terms) 
    {
        terms.prefix("_partition_" + terms.id());
        
        IndexOnDisk index = (IndexOnDisk) IndexFactory.of(src_ref);
        
        Compressor bc = (!with_pos) 
            ? new BasicCompressor(index, dst_ref)
            : new BlockCompressor(index, dst_ref);
            
        try {
            bc.compress(terms);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return terms;
    }
}