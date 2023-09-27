# Development

Any contribution is welcomed!
There are no real formal contribution guidelines right now, they will maybe come later.

## Repository Structure

- [core](core) - Shared core classes of the XPipe Java API, XPipe extensions, and the XPipe daemon implementation.
  This mainly concerns API classes not a lot of implementation.
- [beacon](beacon) - The XPipe beacon component is responsible for handling all communications between the XPipe
  daemon and the client applications, for example APIs and the CLI
- [app](app) - Contains the XPipe daemon implementation, the XPipe desktop application, and an
  API to create all different kinds of extensions for the XPipe platform
- [dist](dist) - Tools to create a distributable package of XPipe
- [ext](ext) - Available XPipe extensions. Essentially every concrete feature implementation is implemented as an extension

## Modularity

All XPipe components target [Java 20](https://openjdk.java.net/projects/jdk/20/) and make full use of the Java Module System (JPMS).
All components are modularized, including all their dependencies.
In case a dependency is (sadly) not modularized yet, module information is manually added using [moditect](https://github.com/moditect/moditect-gradle-plugin).
Further, note that as this is a pretty complicated Java project that fully utilizes modularity,
many IDEs still have problems building this project properly.

For example, you can't build this project in eclipse or vscode as it will complain about missing modules.
The tested and recommended IDE is IntelliJ.
When setting up the project in IntelliJ, make sure that the correct JDK (Java 20)
is selected both for the project and for gradle itself.

## Setup

You need to have an up-to-date version of XPipe installed on your local system in order to properly
run XPipe in a development environment.
This is due to the fact that some components are only included in the release version and not in this repository.
XPipe is able to automatically detect your local installation and fetch the required
components from it when it is run in a development environment.

You need to have GraalVM Community Edition for Java 20 installed as a JDK to compile the project.
If you are on Linux or macOS, you can easily accomplish that by running the `setup.sh` script.
On Windows, you have to manually install the JDK.

## Building and Running

You can use the gradle wrapper to build and run the project:
- `gradlew app:run` will run the desktop application. You can set various useful properties in `app/build.gradle`
- `gradlew dist` will create a distributable production version in `dist/build/dist/base`.
- `gradlew <project>:test` will run the tests of the specified project.

You are also able to properly debug the built production application through two different methods:
- The `dist/build/dist/base/app/scripts/xpiped_debug` script will launch the application in debug mode and with a console attached to it
- The `dist/build/dist/base/app/scripts/xpiped_debug_attach` script attaches a debugger with the help of [AttachMe](https://plugins.jetbrains.com/plugin/13263-attachme).
  Just make sure that the attachme process is running within IntelliJ, and the debugger should launch automatically once you start up the application.

Note that when any unit test is run using a debugger, the XPipe daemon process that is started will also attempt
to connect to that debugger through [AttachMe](https://plugins.jetbrains.com/plugin/13263-attachme) as well.
