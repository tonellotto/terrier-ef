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

import org.terrier.structures.Index;


class CompressorMapper implements Function<TermPartition,TermPartition>
{
	private final String src_index_path, src_index_prefix, dst_index_path, dst_index_prefix;
	private final boolean with_pos;
	
	public CompressorMapper(final String src_index_path, final String src_index_prefix, final String dst_index_path, final String dst_index_prefix, final boolean with_pos) 
	{
		this.src_index_path = src_index_path;
		this.src_index_prefix = src_index_prefix;
		this.dst_index_path = dst_index_path;
		this.dst_index_prefix = dst_index_prefix;
		this.with_pos = with_pos;
	}

	@Override
	public TermPartition apply(TermPartition terms) 
	{
		String this_prefix = dst_index_prefix + "_partition_" + terms.id();
		terms.setPrefix(this_prefix);
		Compressor bc = (!with_pos) 
			? new BasicCompressor(Index.createIndex(src_index_path, src_index_prefix), dst_index_path, dst_index_prefix)
			: new BlockCompressor(Index.createIndex(src_index_path, src_index_prefix), dst_index_path, dst_index_prefix);
		try {
			bc.compress(terms);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return terms;
	}	
}