## X-Pipe Java

This repository contains the following four modules:

- Core - Shared core classes of the Java API and the X-Pipe daemon implementation
- API - The API that can be used to interact with X-Pipe from any JVM-based languages
- Beacon - The X-Pipe beacon component is responsible for handling all communications between the X-Pipe daemon
  and the client applications, for example the various programming language APIs and the CLI
- Extension - An API to create all different kinds of extensions for the X-Pipe platform

## Development

All X-Pipe components target [JDK 17](https://openjdk.java.net/projects/jdk/17/) and make full use of the Java Module System (JPMS).
All components are modularized, including all their dependencies.
In case a dependency is (sadly) not modularized yet, module information is manually added using [moditect](https://github.com/moditect/moditect-gradle-plugin).
These dependency generation rules are accumulated in the [X-Pipe dependencies](https://github.com/xpipe-io/xpipe_java_deps)
repository, which is shared between all components and integrated as a git submodule.
