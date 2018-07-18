# Elias-Fano Compression in Terrier 5

This package provides Elias-Fano compression for docids, frequencies and positions in Terrier 5. At its core, it is a refactoring of the Elias-Fano compression included in the [MG4J](mg4j.di.unimi.it).

The package name is `terrier-eliasfano`, with current version 1.5.

## Elias-Fano Inverted Index (TO BE UPDATED)

This package plugs the encoding-decoding procedures for quasi-succinct indexes implemented by MG4J into the Terrier index data structures.

Given a Terrier plain old index, the following code can be used to generate a new quasi-succinct index compatible with Terrier 5 APIs:

    mvn clean install

Note that this package does not produce an *uberjar*.

To convert an existing index:

    java -Xmx8G -ea -server -cp \
        indexer/target/terrier-eliasfano-indexer-2.0-jar-with-dependencies.jar \
        it.cnr.isti.hpclab.ef.Generator /Users/khast/index-java cw09b    

The output quasi-succinct index will have the prefix `cw09b.ef`.
