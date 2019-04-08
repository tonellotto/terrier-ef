# Elias-Fano Compression in Terrier 5

[![Build Status](https://travis-ci.org/tonellotto/terrier-ef.svg?branch=1.5.1)](https://travis-ci.org/tonellotto/terrier-ef)

This package provides Elias-Fano compression for docids, frequencies and positions in Terrier 5. At its core, it is a refactoring of the Elias-Fano compression included in the [MG4J](http://mg4j.di.unimi.it) free full-text search engine for large document collections written in Java, and described in the [paper](https://dl.acm.org/citation.cfm?id=2433409):

	@inproceedings{Vigna:2013:QI:2433396.2433409,
		author = {Vigna, Sebastiano},
		title = {Quasi-succinct Indices},
		booktitle = {Proceedings of the Sixth ACM International Conference on Web Search and Data Mining},
		series = {WSDM '13},
		year = {2013},
		isbn = {978-1-4503-1869-3},
		location = {Rome, Italy},
		pages = {83--92},
		numpages = {10},
		doi = {10.1145/2433396.2433409},
		acmid = {2433409},
		publisher = {ACM},
		address = {New York, NY, USA},
		keywords = {compressed indices, succinct data structures},
	}

This package is [free software](http://www.gnu.org/philosophy/free-sw.html) distributed under the [GNU Lesser General Public License](http://www.gnu.org/copyleft/lesser.html).

## Pre-requisites

None.

## Generating an Elias-Fano Inverted Index using CLITools

This package plugs the encoding-decoding procedures for quasi-succinct indexes implemented by MG4J into the Terrier index data structures.

Given a Terrier plain old index, the following steps can be used to generate a new quasi-succinct index compatible with Terrier 5 APIs.

If not already available, e.g. from Maven Central, you should git clone and install terrier-eliasfano:

	mvn -DskipTests clean install

Tell Terrier that you wish to add a plugin, by appending the following to your terrier.properties file in your Terrier distribution:

    terrier.mvn.coords=it.cnr.isti.hpclab:terrier-eliasfano:1.5

Then, to convert an existing index:

	bin/terrier ef-recompress /path/to/new/index cw09b

The output quasi-succinct index will have the prefix `cw09b`. You can change the source index using the `-I` option, e.g.,

    bin/terrier ef-recompress -I /path/to/old/index/data.properties /path/to/new/index cw09b

The degree of parallelism and whether block positions should be compressed are varied using the `-p` and `-b` options, respectively. You can view the help information for ef-recompress:

	bin/terrier help ef-recompress

## Generating an Elias-Fano Inverted Index using scripts

This package plugs the encoding-decoding procedures for quasi-succinct indexes implemented by MG4J into the Terrier index data structures.

Given a Terrier plain old index, the following steps can be used to generate a new quasi-succinct index compatible with Terrier 5 APIs.

If not already available, e.g. from Maven Central, you should git clone and install terrier-eliasfano:

	mvn -DskipTests clean package appassembler:assemble

Then, to convert an existing index:

	./target/bin/ef-convert -index /path/to/old/index/cw09b.properties -path /path/to/new/index/ -prefix cw09b.ef

The input index has the prefix `cw09b`. The output quasi-succinct index will have the prefix `cw09b.ef`.

The `ef-convert` tool accepts the following options.

    -path [String] (required)

Path of the directory that will hold the output Terrier index.

    -prefix [String] (required)

Prefix of the output Terrier index. If an index with the given prefix already exists, the execution will be aborted.

    -index [String] (required)

Fully qualified filename of one of the files of a existing Terrier index. The parameter will be split automatically into a Terrier path and prefix.

    -b (optional)

Compress positions with Elias-Fano. Default: false

    -p [Number] (optional)

Number of threads to use. Anyway the maximum value will be the number of available cores. Default: 1.

**Multi-threaded compressions is experimental -- caution advised due to threads competing for available memory!**

## Notes

-   supports (block) positions
-   does not support indices using fields

## Credits

Developed by Nicola Tonellotto, ISTI-CNR. Contributions by Craig Macdonald, University of Glasgow, and Matteo Catena, ISTI-CNR.
