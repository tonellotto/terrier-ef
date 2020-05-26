/*
 * Elias-Fano compression for Terrier 5
 *
 * Copyright (C) 2018-2020 Nicola Tonellotto
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or (at
 *  your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 *  License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */

package it.cnr.isti.hpclab.ef;

/**
 * A final class storing most string constants.
 */
public final class EliasFano {
    /**
     * Restricted instantiation (avoid Interface constants anti-pattern).
     */
    private EliasFano() {
    }

    /** Filename constant. */
    public static final String DOCID_EXTENSION = ".docids";
    /** Filename constant. */
    public static final String FREQ_EXTENSION  = ".freqs";
    /** Filename constant. */
    public static final String POS_EXTENSION  = ".positions";

    /** Filename constant. */
    public static final String USUAL_EXTENSION = ".ef";
    /** Filename constant. */
    public static final String SIZE_EXTENSION  = ".sizes";

    /**
     * Constant used in the index properties file to specify the log2 of
     * the quantum used in elias-fano encoding.
     */
    public static final String LOG2QUANTUM  = "log2Quantum";
    /**
     * Constant used in the index properties file to specify the byte order.
     */
    public static final String BYTEORDER    = "ByteOrder";

    /**
     * Constant used in the index properties file to specify if the index
     * store positional information.
     */
    public static final String HAS_POSITIONS = "hasPostions";
}
