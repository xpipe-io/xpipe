[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.xpipe/core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.xpipe/core)
[![javadoc](https://javadoc.io/badge2/io.xpipe/core/javadoc.svg)](https://javadoc.io/doc/io.xpipe/core)
[![Build Status](https://github.com/xpipe-io/xpipe_java/actions/workflows/core-build.yml/badge.svg)](https://github.com/xpipe-io/xpipe_java/actions/workflows/core-build.yml)


## X-Pipe Core

The X-Pipe core component contains all the shared core classes used by the API, beacon, and daemon.

The main part can be found in the [data package]().
It contains all definitions of the internal X-Pipe data model and all IO functionality for these data structures.

The [source package]() contains the basic data source model classes.
These have to be used by every custom data source implementation.

The [store package]() contains the basic data store model classes.
These have to be used by every custom data store implementation.

Every class is expected to be potentially used in the context of files and message exchanges.
As a result, all data structures exchanged must be serializable/deserializable with jackson.