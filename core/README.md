[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.xpipe/xpipe-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.xpipe/xpipe-core)
[![javadoc](https://javadoc.io/badge2/io.xpipe/xpipe-core/javadoc.svg)](https://javadoc.io/doc/io.xpipe/xpipe-core)

## XPipe Core

The XPipe core module contains all the shared core classes used by the API, beacon, and daemon implementation.
It contains the following packages:

- [charsetter](src/main/java/io/xpipe/core/charsetter): Classes for handling all things charset 
  related such as detection and handling of data streams with BOMs.

- [data](src/main/java/io/xpipe/core/data): Contains all definitions of the
  internal XPipe data model and all the IO functionality to read and write these data structures.
  For more information, see [XPipe data model](https://xpipe-io.readthedocs.io/en/latest/dev/model.html)

- [dialog](src/main/java/io/xpipe/core/dialog): In API to create server/daemon side CLI dialogs.
  These are used by extensions for data source and data store configuration from the command line.

- [source](src/main/java/io/xpipe/core/source): The basic data source classes that are used by every data source implementation.

- [store](src/main/java/io/xpipe/core/store): The basic data store classes that are used by every data store implementation.

- [util](src/main/java/io/xpipe/core/source): A few utility classes for serialization and more.

Every class is expected to be potentially used in the context of files and message exchanges.
As a result, essentially all objects must be serializable/deserializable with jackson.


