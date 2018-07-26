# Elias-Fano Compression in Terrier 5

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
 		url = {http://doi.acm.org/10.1145/2433396.2433409},
 		doi = {10.1145/2433396.2433409},
 		acmid = {2433409},
 		publisher = {ACM},
 		address = {New York, NY, USA},
 		keywords = {compressed indices, succinct data structures},
	}

This package is [free software](http://www.gnu.org/philosophy/free-sw.html) distributed under the [GNU Lesser General Public License](http://www.gnu.org/copyleft/lesser.html).

## Pre-requisites

Terrier 5.0 is required

## Generating an Elias-Fano Inverted Index

This package plugs the encoding-decoding procedures for quasi-succinct indexes implemented by MG4J into the Terrier index data structures.

Given a Terrier plain old index, the following stages can be used to generate a new quasi-succinct index compatible with Terrier 5 APIs.

1. If not already available, e.g. from Maven Central, you should download and install terrier-eliasfano

    mvn clean install

2. Tell Terrier that you wish to add a plugin, by appending the following to your terrier.properties file in your Terrier distribution:

    terrier.mvn.coords=it.cnr.isti.hpclab:terrier-eliasfano:1.5
	
3. Then, to convert an existing index:

    bin/terrier ef-recompress /path/to/new/index cw09b    

The output quasi-succinct index will have the prefix `cw09b`. You can change the source index using the `-I` option, e.g.

    bin/terrier ef-recompress -I /path/to/old/index/data.properties /path/to/new/index cw09b

The degree of parallelism and whether block positions should be compressed are varied using the `-p` and `-b` options, respectively. You can view the help information for ef-recompress:

	bin/terrier help ef-recompress
	
## Notes

- supports (block) positions
- does not support indices using fields
	
## Credits

Developed by Nicola Tonellotto, ISTI-CNR.