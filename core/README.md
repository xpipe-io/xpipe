[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.xpipe/xpipe-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.xpipe/xpipe-core)
[![javadoc](https://javadoc.io/badge2/io.xpipe/xpipe-core/javadoc.svg)](https://javadoc.io/doc/io.xpipe/xpipe-core)

## XPipe Core

The XPipe core module contains all the shared core classes used by the API, beacon, and daemon implementation.
It contains the following packages:

- [dialog](src/main/java/io/xpipe/core/dialog): In API to create server/daemon side CLI dialogs.

- [store](src/main/java/io/xpipe/core/store): The basic data store classes that are used by every data store implementation.

- [process](src/main/java/io/xpipe/core/process): Base classes for the shell process handling implementation.

- [util](src/main/java/io/xpipe/core/source): A few utility classes for serialization and more.

Every class is expected to be potentially used in the context of files and message exchanges.
As a result, essentially all objects must be serializable/deserializable with jackson.


