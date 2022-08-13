[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.xpipe/core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.xpipe/core)
[![javadoc](https://javadoc.io/badge2/io.xpipe/core/javadoc.svg)](https://javadoc.io/doc/io.xpipe/core)

## X-Pipe Core

The X-Pipe core module contains all the shared core classes used by the API, beacon, and daemon implementation.

The main component is the [data package](src/main/java/io/xpipe/core/data).
It contains all definitions of the internal X-Pipe data model and all IO functionality for these data structures.

The [source package](src/main/java/io/xpipe/core/source) contains the basic data source classes,
which are used by every data source implementation.

The [store package](src/main/java/io/xpipe/core/store) contains the basic data store classes,
which are used by every data store implementation.

Every class is expected to be potentially used in the context of files and message exchanges.
As a result, all data structures exchanged must be serializable/deserializable with jackson.


