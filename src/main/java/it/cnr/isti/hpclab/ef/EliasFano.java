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

/**
 * An interface storing most string constants.
 */
public interface EliasFano 
{
	static final String DOCID_EXTENSION = ".docids";
	static final String FREQ_EXTENSION  = ".freqs";
	static final String POS_EXTENSION  = ".positions";
	
	static final String USUAL_EXTENSION = ".ef";
	static final String SIZE_EXTENSION  = ".sizes";
	
	static final String LOG2QUANTUM  = "log2Quantum";
	static final String BYTEORDER    = "ByteOrder";
	
	static final String HAS_POSITIONS = "hasPostions";
}
