<img src="https://user-images.githubusercontent.com/72509152/213873342-7638e830-8a95-4b5d-ad3e-5a9a0b4bf538.png" alt="drawing" width="300"/>

As the name suggests, X-Pipe (short for eXtended Pipe) has
the goal of improving on the established concept of pipes.
As with normal pipes, the main goal of X-Pipe essentially is the transfer of data from producers to consumers.
It focuses on the following three ideas:

- Could we support connections to any remote system with pipes instead of limiting yourself to your local system?
  If so, we should focus on supporting already existing tools
  to establish remote connections instead of reinventing the wheel?

- Could we support more than just transferring raw bytes and text?
  Why not work on a higher level of abstraction instead,
  which would allow for a connection between producers and consumers
  that work on the same type of data, e.g. a table, even though the underlying formats are different.

- Could we make the process as user friendly as possible?
  Most tools in that space grow to be incredible complex and make it very difficult for users to get started.
  The goal is too provide a use a friendly alternative that almost anyone can use instantly.

X-Pipe consists out of two main components that achieve these goals:

- The Connection Explorer provides the ability to flexibly connect to any remote system

- The Data Explorer then builds on top of it to allow you to smartly work with all kinds of data

Note that this project is still in early development!

## Connection Explorer

The connection explorer allows you to connect to, manage, and interact with all kinds of remote systems.

<img src="https://user-images.githubusercontent.com/72509152/213240153-3f742f03-1289-44c3-bf4d-626d9886ffcf.png" alt="drawing" height="450"/>

It comes with the following main features:

#### Ultra Flexible Connector

- Can connect to standard servers, database servers, and more

- Supports established protocols (e.g. SSH and more) plus any custom connection methods that work through the command-line

- Is able to integrate any kind of proxies into the connection process, even ones with different protocols

#### Instant launch for remote shells and commands

- Automatically login into a shell in your favourite terminal with one click (no need to fill password prompts, etc.)

- Works for all kinds of shells. This includes command shells (e.g. bash, PowerShell, cmd, etc.) and database shells (e.g. PSQL Shell)

- Comes with integrations for all commonly used terminals in Windows and Linux

- Exclusively uses established CLI tools and therefore works out of the box on most systems and doesn't require any additional setup

#### Connection Manager

- Easily create and manage all kinds of remote connections

- Securely stores all information exclusively on your computer and encrypts all secret information

- Allows you to share connections and their information to any other trusted system in your network

## Data Explorer

Building on top of the connection explorer, the data explorer
allows you to manage and work with all kinds of data sources:

<img src="https://user-images.githubusercontent.com/72509152/213240736-7a27fb3c-e8c3-4c92-bcea-2a782e53dc31.png" alt="drawing" height="450"/>

#### Work with your data on a higher level

- X-Pipe utilizes structures of data instead of the raw data itself, allowing for
  a higher level workflow that is independent of the underlying data format

- Save time when adding data sources by making use of the advanced
  auto detection feature of X-Pipe where you don't have to worry about encodings, format configurations, and more

- Easily convert between different data representations

#### Integrate X-Pipe with your favorite tools and workflows

- Easily import and export all kinds of data formats and technologies

- Access data sources from the commandline with the X-Pipe CLI or
  your favorite programming languages using the X-Pipe API

- Connect select third party applications directly to X-Pipe through extensions

### Summary

Even though X-Pipe comes with wide variety of features and components, it essentially still only focuses on one goal:

*To get your data from A to B in the easiest way possible while also preserving
compatibility through intermediation.
For that, you can use the medium you like the most, whether that is a GUI, a CLI, or an API.*

Essentially, X-Pipe aims to make the transfer process as quick as possible
so you can spend more time actually working with your
data instead of figuring out how to transfer it.
X-Pipe can therefore be a massive timesaver for
anyone who interacts with a wide range of data.
In case this sounds interesting to you, take a look at the
complete installation instructions and the user guide that can be
found in the [X-Pipe Documentation](https://docs.xpipe.io/guide/installation.html).

## Repository Structure

The following for modules make up the X-Pipe API and a licensed under the MIT license:
- [core](core) - Shared core classes of the X-Pipe Java API, X-Pipe extensions, and the X-Pipe daemon implementation
- [API](api) - The API that can be used to interact with X-Pipe from any JVM-based language.
  For setup instructions, see the [X-Pipe Java API Usage](https://xpipe-io.readthedocs.io/en/latest/dev/api/java.html) section.
- [beacon](beacon) - The X-Pipe beacon component is responsible for handling all communications between the X-Pipe daemon
  and the client applications, for example the various programming language APIs and the CLI
- [extension](extension) - An API to create all different kinds of extensions for the X-Pipe platform
  For setup instructions, see the [X-Pipe extension development](https://xpipe-io.readthedocs.io/en/latest/dev/extensions/index.html) section.

The other modules make up the X-Pipe implementation and are licensed under GPL:
- [app](app) - Contains the X-Pipe daemon implementation and the X-Pipe desktop application code
- [cli](cli) - The X-Pipe CLI implementation, a GraalVM native image application
- [dist](dist) - Tools to create a distributable package of X-Pipe
- [ext](ext) - Available X-Pipe extensions. Note that essentially every feature is implemented as an extension


## Development

Any contribution is welcomed!
There are no real formal contribution guidelines right now, they will maybe come later.

### Modularity

All X-Pipe components target [JDK 19](https://openjdk.java.net/projects/jdk/19/) and make full use of the Java Module System (JPMS).
All components are modularized, including all their dependencies.
As the CLI utilizes the native image capability of [GraalVM](https://github.com/graalvm/graalvm-ce-builds/releases/tag/vm-22.3.0), it is recommended to use GraalVM with Java 19 support.
In case a dependency is (sadly) not modularized yet, module information is manually added using [moditect](https://github.com/moditect/moditect-gradle-plugin).
Further, note that as this is a pretty complicated Java project that fully utilizes modularity,
many IDEs still have problems building this project properly.
For example, you can't build this project in eclipse or vscode as it will complain about missing modules.
The tested and recommended IDE is intellij.

### Building and Running

You can use the gradle wrapper to build and run the project:
- `gradlew app:run` will run the desktop application. You can set various useful properties in `app/build.gradle`
- `gradlew builtCli` will create a native image for the CLI application
- `gradlew dist` will create a distributable production version in `dist/build/dist/base`.
  To include this CLI executable in this build, make sure to run `gradlew builtCli` first
- You can also run the CLI application in development mode with something like `gradlew :cli:clean :cli:run --args="daemon start"`.
  Note here that you should always clean the CLI project first, as the native image plugin is a little buggy in that regard.
- `gradlew <project>:test` will run the tests of the specified project. 

Some unit tests depend on a connection to an X-Pipe daemon to properly function.
To launch the installed daemon, it is required that you either have X-Pipe
installed or have set the `XPIPE_HOME` environment variable in case you are using a portable version.

You are also able to properly debug the built production application through two different methods:
- The `app/scripts/xpiped_debug` script will launch the application in debug mode and with a console attached to it
- The `app/scripts/xpiped_debug_attach` script attaches a debugger with the help of [AttachMe](https://plugins.jetbrains.com/plugin/13263-attachme).
  Just make sure that the attachme process is running within IntelliJ, and the debugger should launch automatically once you start up the application.

Note that when any unit test is run using a debugger, the X-Pipe daemon process that is started will also attempt
to connect to that debugger through [AttachMe](https://plugins.jetbrains.com/plugin/13263-attachme) as well.

