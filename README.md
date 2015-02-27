# Elias-Fano Compression in Terrier 4

This package provides Elias-Fano compression for docids and frequencies in Terrier 4. It is heavily based on the MG4J implementation provided by Sebastiano Vigna. 

It depends on the following packages:

* `terrier-skipping`, version 1.4.0
* `mg4j-big` , version 5.2.1

The package name is `terrier-succinct`, with current version 1.4.0.

## Quasi-Succinct Inverted Index

This package plugs the encoding-decoding procedures for quasi-succinct indexes implemented by MG4J into the Terrier index data structures.

Given a Terrier plain old index, the following code can be used to generate a new quasi-succinct index compatible with Terrier 4 APIs:

    mvn clean install
    
Note that this package does not produce an *uberjar*.