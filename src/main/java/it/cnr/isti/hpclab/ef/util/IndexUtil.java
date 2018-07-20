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
package it.cnr.isti.hpclab.ef.util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Properties;

import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

import it.cnr.isti.hpclab.ef.EliasFano;

public class IndexUtil 
{
	public static int DEFAULT_MAX_TERM_LENGTH = ApplicationSetup.MAX_TERM_LENGTH;
	
	/**
	 * This static method is used to generate a properties file for a Elias-Fano Terrier index including only "default" data structures.
	 * <p>
	 * This means that the only data structures available through the generate file will be the lexicon, the document index and the inverted index.
	 * <p>
	 * The only global statistics included (for weighting models) are the number of documents (to be provided) and the number of terms (to be provided).
	 * 
	 * @param filename the filename to store properties in, must explicitly end with <code>.properties</code>
	 * @param num_docs the number of documents in the referring index
	 * @param num_terms the number of terms in the referring index
	 * @param log2quatum the log2 value of the quantum used to build the referring index
	 * 
	 * @throws IOException if something wrong at I/O level happens
	 */
	public static void writeEFIndexProperties(final String filename, final int num_docs, final int num_terms, final long num_pointers, final long num_tokens, final int log2quantum) throws IOException
	{
		File file = new File(filename);
		
		if (file.isDirectory())
			throw new IllegalArgumentException("The filename provided (" + filename + ") is a directory");

		Properties properties = new Properties();

		properties.setProperty("index.terrier.version", "5.0");
		
		properties.setProperty("num.Documents", Integer.toString(num_docs));
		properties.setProperty("num.Terms",     Integer.toString(num_terms));
		properties.setProperty("num.Pointers",  Long.toString(num_pointers));
		properties.setProperty("num.Tokens",    Long.toString(num_tokens));
		
		properties.setProperty(EliasFano.LOG2QUANTUM, Integer.toString(log2quantum));
		properties.setProperty(EliasFano.BYTEORDER,   ByteOrder.nativeOrder().toString());
		
		properties.setProperty("max.term.length",Integer.toString(DEFAULT_MAX_TERM_LENGTH));
		
		properties.setProperty("index.lexicon.termids", "aligned");
		properties.setProperty("index.lexicon.bsearchshortcut", "default");

		properties.setProperty("index.lexicon.class",		     "org.terrier.structures.FSOMapFileLexicon");
		properties.setProperty("index.lexicon.parameter_types",  "java.lang.String,org.terrier.structures.IndexOnDisk");
		properties.setProperty("index.lexicon.parameter_values", "structureName,index");

		properties.setProperty("index.lexicon-keyfactory.class",            "org.terrier.structures.seralization.FixedSizeTextFactory");
		properties.setProperty("index.lexicon-keyfactory.parameter_types",  "java.lang.String");
		properties.setProperty("index.lexicon-keyfactory.parameter_values", "${max.term.length}");
		
		properties.setProperty("index.lexicon-valuefactory.class",            "it.cnr.isti.hpclab.ef.structures.EFLexiconEntry$Factory");
		properties.setProperty("index.lexicon-valuefactory.parameter_values", "");
		properties.setProperty("index.lexicon-valuefactory.parameter_types",  "");

		properties.setProperty("index.document.class",            "it.cnr.isti.hpclab.ef.structures.EFDocumentIndex");
		properties.setProperty("index.document.parameter_types",  "org.terrier.structures.IndexOnDisk");
		properties.setProperty("index.document.parameter_values", "index");

		properties.setProperty("index.inverted.class", 		      "it.cnr.isti.hpclab.ef.structures.EFInvertedIndex");
		properties.setProperty("index.inverted.parameter_types",  "org.terrier.structures.IndexOnDisk,org.terrier.structures.DocumentIndex");
		properties.setProperty("index.inverted.parameter_values", "index,document");
		
		// properties.setProperty("index.has.blocks", "false");

		properties.store(Files.writeFileStream(filename),"");
	}
	
	public static void writeEFPosIndexProperties(final String filename, final int num_docs, final int num_terms, final long num_pointers, final long num_tokens, final int log2quantum) throws IOException
	{
		File file = new File(filename);
		
		if (file.isDirectory())
			throw new IllegalArgumentException("The filename provided (" + filename + ") is a directory");

		Properties properties = new Properties();

		properties.setProperty("index.terrier.version", "5.0");
		
		properties.setProperty("num.Documents", Integer.toString(num_docs));
		properties.setProperty("num.Terms",     Integer.toString(num_terms));
		properties.setProperty("num.Pointers",  Long.toString(num_pointers));
		properties.setProperty("num.Tokens",    Long.toString(num_tokens));
		
		properties.setProperty(EliasFano.LOG2QUANTUM, Integer.toString(log2quantum));
		properties.setProperty(EliasFano.BYTEORDER,   ByteOrder.nativeOrder().toString());
		properties.setProperty(EliasFano.HAS_POSITIONS, "true");
		
		properties.setProperty("max.term.length",Integer.toString(DEFAULT_MAX_TERM_LENGTH));
		
		properties.setProperty("index.lexicon.termids", "aligned");
		properties.setProperty("index.lexicon.bsearchshortcut", "default");

		properties.setProperty("index.lexicon.class",		     "org.terrier.structures.FSOMapFileLexicon");
		properties.setProperty("index.lexicon.parameter_types",  "java.lang.String,org.terrier.structures.IndexOnDisk");
		properties.setProperty("index.lexicon.parameter_values", "structureName,index");

		properties.setProperty("index.lexicon-keyfactory.class",            "org.terrier.structures.seralization.FixedSizeTextFactory");
		properties.setProperty("index.lexicon-keyfactory.parameter_types",  "java.lang.String");
		properties.setProperty("index.lexicon-keyfactory.parameter_values", "${max.term.length}");
		
		properties.setProperty("index.lexicon-valuefactory.class",            "it.cnr.isti.hpclab.ef.structures.EFBlockLexiconEntry$Factory");
		properties.setProperty("index.lexicon-valuefactory.parameter_values", "");
		properties.setProperty("index.lexicon-valuefactory.parameter_types",  "");

		properties.setProperty("index.document.class",            "it.cnr.isti.hpclab.ef.structures.EFDocumentIndex");
		properties.setProperty("index.document.parameter_types",  "org.terrier.structures.IndexOnDisk");
		properties.setProperty("index.document.parameter_values", "index");

		properties.setProperty("index.inverted.class", 		      "it.cnr.isti.hpclab.ef.structures.EFInvertedIndex");
		properties.setProperty("index.inverted.parameter_types",  "org.terrier.structures.IndexOnDisk,org.terrier.structures.DocumentIndex");
		properties.setProperty("index.inverted.parameter_values", "index,document");

		// properties.setProperty("index.has.blocks", "true");
		
		properties.store(Files.writeFileStream(filename),"");
	}}
