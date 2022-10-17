[![Build Status](https://github.com/xpipe-io/xpipe_java/actions/workflows/build.yml/badge.svg)](https://github.com/xpipe-io/xpipe_java/actions/workflows/build.yml)
[![Publish Status](https://github.com/xpipe-io/xpipe_java/actions/workflows/publish.yml/badge.svg)](https://github.com/xpipe-io/xpipe_java/actions/workflows/publish.yml)

## X-Pipe Java

The fundamental components of the [X-Pipe project](https://docs.xpipe.io).
This repository contains the following four modules:

- Core - Shared core classes of the Java API, extensions, and the X-Pipe daemon implementation
- API - The API that can be used to interact with X-Pipe from any JVM-based languages
- Beacon - The X-Pipe beacon component is responsible for handling all communications between the X-Pipe daemon
  and the client applications, for example the various programming language APIs and the CLI
- Extension - An API to create all different kinds of extensions for the X-Pipe platform

## Installation / Usage

The *core* and *extension* libraries are used in X-Pipe extension development.
For setup instructions, see the [X-Pipe extension development](https://xpipe-io.readthedocs.io/en/latest/dev/extensions.html) section.

The *beacon* library handles all communication and serves as a
reference when implementing an API or program that communicates with the X-Pipe daemon.

The *api* library serves as a reference implementation for other potential X-Pipe APIs
and is also used to enable your Java program to communicate with X-Pipe.
For setup instructions, see the [X-Pipe Java API Usage]() section.

## Development

All X-Pipe components target [JDK 17](https://openjdk.java.net/projects/jdk/17/) and make full use of the Java Module System (JPMS).
All components are modularized, including all their dependencies.
In case a dependency is (sadly) not modularized yet, module information is manually added using [moditect](https://github.com/moditect/moditect-gradle-plugin).
These dependency generation rules are accumulated in the [X-Pipe dependencies](https://github.com/xpipe-io/xpipe_java_deps)
repository, which is shared between all components and integrated as a git submodule.

Some unit tests depend on a connection to an X-Pipe daemon to properly function.
To launch the installed daemon, it is required that the `XPIPE_HOME` environment variable
is set or the `xpipe` / `xpipe.exe` CLI executable is added to the `PATH` variable.

