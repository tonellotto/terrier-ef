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

public class TermPartition implements Comparable<TermPartition>
{
	private int id;
	private String prefix;
	public final int begin;
	public final int end;
	
	private TermPartition(final int begin, final int end, final int id)
	{
		this.begin = begin;
		this.end = end;
		this.id = id;
	}
	
	public String toString()
	{
		return id + " [" + begin + "," + end +"] (" + prefix + ")";
	}
	
	public void setPrefix(final String prefix)
	{
		this.prefix = prefix;
	}
	
	public String prefix()
	{
		return this.prefix;
	}

	public void setId(final int id)
	{
		this.id = id;
	}
	
	public int id()
	{
		return this.id;
	}

	public static TermPartition[] split(final int max, final int bins)
	{
		TermPartition[] res = new TermPartition[bins];
		for (int id = 0; id < bins; ++id)
			res[id] = new TermPartition(max * id / bins, max * (id+1) / bins, id);
		return res;
	}

	@Override
	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TermPartition other = (TermPartition) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public int compareTo(TermPartition o) 
	{
		return Integer.compare(this.id,  o.id);
	}
}