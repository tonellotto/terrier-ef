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

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
// import org.junit.Test;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;

public class IndexCreationDebug extends EFSetupTest
{
	private Index index = null;
	
	@Before public void createIndex() throws Exception
	{
		super.doShakespeareIndexing();
		index = Index.createIndex();
		
	}
	
	@After public void deleteIndex() throws IOException
	{
		index.close();
	} 
	
	//@Test 
	public void printPostingList() throws IOException
	{
		Lexicon<String> lex = index.getLexicon();
		
		LexiconEntry le = lex.getLexiconEntry("new");
		System.err.println(le);
		
		IterablePosting p = index.getInvertedIndex().getPostings(le);
		while (p.next() != IterablePosting.EOL)
		{
			System.err.print(p + ", ");
		}
		System.err.println();
	}
	
}
