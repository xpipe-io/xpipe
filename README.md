[![Build Status](https://github.com/xpipe-io/xpipe_java/actions/workflows/build.yml/badge.svg)](https://github.com/xpipe-io/xpipe_java/actions/workflows/build.yml)
[![Publish Status](https://github.com/xpipe-io/xpipe_java/actions/workflows/publish.yml/badge.svg)](https://github.com/xpipe-io/xpipe_java/actions/workflows/publish.yml)

## X-Pipe Java

The fundamental components of the [X-Pipe project](https://xpipe.io).
This repository contains the following four modules:

- Core - Shared core classes of the X-Pipe Java API, X-Pipe extensions, and the X-Pipe daemon implementation
- API - The API that can be used to interact with X-Pipe from any JVM-based language10
- Beacon - The X-Pipe beacon component is responsible for handling all communications between the X-Pipe daemon
  and the client applications, for example the various programming language APIs and the CLI
- Extension - An API to create all different kinds of extensions for the X-Pipe platform

## Installation / Usage

The *core* and *extension* modules are used in X-Pipe extension development.
For setup instructions, see the [X-Pipe extension development](https://xpipe-io.readthedocs.io/en/latest/dev/extensions/index.html) section.

The *beacon* module handles all communication and serves as a
reference when implementing the communication of an API or program that interacts with the X-Pipe daemon.

The *api* module serves as a reference implementation for other potential X-Pipe APIs
and can also be used to access X-Pipe functionalities from your Java programs.
For setup instructions, see the [X-Pipe Java API Usage](https://xpipe-io.readthedocs.io/en/latest/dev/api/java/index.html) section.

## Development Notes

All X-Pipe components target [JDK 17](https://openjdk.java.net/projects/jdk/17/) and make full use of the Java Module System (JPMS).
All components are modularized, including all their dependencies.
In case a dependency is (sadly) not modularized yet, module information is manually added using [moditect](https://github.com/moditect/moditect-gradle-plugin).
These dependency generation rules are accumulated in the [X-Pipe dependencies](https://github.com/xpipe-io/xpipe_java_deps)
repository, which is shared between all components and integrated as a git submodule.

Some unit tests depend on a connection to an X-Pipe daemon to properly function.
To launch the installed daemon, it is required that you either have X-Pipe
installed or have set the `XPIPE_HOME` environment variable in case you are using a portable version.
