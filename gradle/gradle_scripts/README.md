## Gradle Scripts

This directory contains helper scripts and Java module generation rules for dependencies used by various X-Pipe gradle projects.
It also contains various other types of shared build script components that are useful.

As the [jlink](https://docs.oracle.com/en/java/javase/17/docs/specs/man/jlink.html) tool
effectively requires proper modules as inputs but many established java
libraries did not add proper support yet, using an approach like this is required.
The modules are generated with the help of [moditect](https://github.com/moditect/moditect-gradle-plugin).
The generated `module-info.java` file contains the necessary declarations to make a library work.
While gradle already has a [similar system](https://docs.gradle.org/current/userguide/platforms.html)
to better share dependencies, this system is lacking several features.
For one, it can't pass any other customizations to the build that are required by the dependencies,
e.g. compiler parameters or annotation processors.