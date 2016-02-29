# Elias-Fano Compression in Terrier 4

This package provides Elias-Fano compression for docids and frequencies in Terrier 4. It is heavily based on the MG4J implementation provided by Sebastiano Vigna. 

It is composed by two submodules:

* `reader`, providing data structure and algorithms to correctly read an Elias-Fano compressed index
* `indexer`, providing data structure and algorithms to convert a standard Terrier index into an Elias-Fano compressed index

It depends on the following packages:

* `terrier-skipping-reader`
* `terrier-reader`

The package name is `terrier-eliasfano`, with current version 2.0.

## Elias-Fano Inverted Index

This package plugs the encoding-decoding procedures for quasi-succinct indexes implemented by MG4J into the Terrier index data structures.

Given a Terrier plain old index, the following code can be used to generate a new quasi-succinct index compatible with Terrier 4 APIs:

    mvn clean install

Note that this package does not produce an *uberjar*.

To convert an existing index:

    java -Xmx8G -ea -server -cp \
        indexer/target/terrier-eliasfano-indexer-2.0-jar-with-dependencies.jar \
        it.cnr.isti.hpclab.ef.Generator /Users/khast/index-java cw09b    

The output quasi-succinct index will have the prefix `cw09b.ef`.