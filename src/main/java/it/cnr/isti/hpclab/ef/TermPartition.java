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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter @Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TermPartition implements Comparable<TermPartition> 
{
    @EqualsAndHashCode.Include
    private int id;
    private String prefix;
    
    private final int begin;
    private final int end;
    
    private TermPartition(final int begin, final int end, final int id) 
    {
        this.begin = begin;
        this.end = end;
        this.id = id;
    }
    
    @Override
    public String toString() {
        return id + " [" + begin + "," + end + "] (" + prefix + ")";
    }

    public static TermPartition[] split(final int max, final int bins) 
    {
        TermPartition[] res = new TermPartition[bins];
        for (int id = 0; id < bins; ++id) {
            res[id] = new TermPartition(max * id / bins, max * (id + 1) / bins, id);
        }
        return res;
    }

    @Override
    public int compareTo(TermPartition o)  {
        return Integer.compare(this.id,  o.id);
    }
}