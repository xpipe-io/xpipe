<img src="https://user-images.githubusercontent.com/72509152/213873342-7638e830-8a95-4b5d-ad3e-5a9a0b4bf538.png" alt="drawing" width="300"/>

### Next level remote data workflows for everyone

X-Pipe is a tool in an early alpha for working with remote connections and their data.
The focus lies on providing:
- An easy remote connection setup and workflow
- Organization of all your connections in one place
- The ability to automatically launch your remote connections
- Data intermediation capabilities to be able to work with and transfer more than just bytes and text

The core idea is to utilize and integrate well with other popular tools and workflows,
focusing on augmenting them rather than replacing them.
X-Pipe is built around existing tools and tries to outsource tasks to them,
such that you can always use your favorite tools to work with X-Pipe, e.g.
text/code editors, terminals, shells, command-line tools and more.
The X-Pipe platform is open source and designed to be extensible, allowing anyone
to implement custom functionality through custom extensions with the help of an exhaustive API.

## Connection Explorer

The connection explorer allows you to connect to, manage, and interact with all kinds of remote systems.
It comes with the following main features:

#### Flexible Connector

- Can connect to standard servers, database servers, and more
- Supports established protocols (e.g. SSH and more) plus any custom connection methods that work through the command-line
- Is able to integrate any kind of proxies into the connection process, even ones with different protocols

#### Instant launch for remote shells and commands

- Automatically login into a shell in your favourite terminal with one click (no need to fill password prompts, etc.)
- Works for all kinds of shells. This includes command shells (e.g. bash, PowerShell, cmd, etc.) and database shells (e.g. PSQL Shell)
- Comes with integrations for all commonly used terminals for all operating systems
- Exclusively uses established CLI tools and therefore works out of the box on most systems and doesn't require any additional setup
- Allows you to customize the launched shell's init environment

#### All your connections in one place

- Easily create and manage all kinds of remote connections at one location
- Securely stores all information exclusively on your computer and encrypts all secret information
- Share connection configurations to any other trusted party through shareable URLs
- Create desktop shortcuts to open your connections

<img src="https://user-images.githubusercontent.com/72509152/213240153-3f742f03-1289-44c3-bf4d-626d9886ffcf.png" alt="drawing" height="450"/>

## Data Explorer

Building on top of the connection explorer, the data explorer
allows you to manage and work with all kinds of data sources:

#### Work with your data on a higher level

- X-Pipe utilizes structures of data instead of the raw data itself, allowing for
  a higher level workflow that is independent of the underlying data format
- Save time when adding data sources by making use of the advanced
  auto detection feature of X-Pipe where you don't have to worry about encodings, format configurations, and more
- Easily convert between different data representations

#### Integrate X-Pipe with your favorite tools and workflows

- Easily import and export all kinds of data formats and technologies
- Access data sources from the command-line with the X-Pipe CLI or
  your favorite programming languages using the X-Pipe APIs
- Connect select third party applications directly to X-Pipe through extensions

<img src="https://user-images.githubusercontent.com/72509152/218159305-64e2ac2c-2d01-4087-89d2-907f2e3a6bed.png" alt="drawing" height="450"/>

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
- [dist](dist) - Tools to create a distributable package of X-Pipe
- [ext](ext) - Available X-Pipe extensions. Essentially every feature is implemented as an extension

### Open source model

X-Pipe utilizes an open core model, which essentially means that
the main application core is open source while certain other components are not.
In this case these non open source components are planned to be future parts of a potential commercialization.
Furthermore, some tests and especially test environments and that run on private servers
are also not included in this repository (Don't want to leak server information).
Finally, scripts and workflows to create signed executables and installers
are also not included to prevent attackers from easily impersonating the shipping the X-Pipe application malware.

The license model is chosen in such a way that you are
able to use and integrate X-Pipe within your application through the MIT-licensed API.
In any other case where you plan to contribute to the X-Pipe platform itself, which is GPL licensed,
I would still have to figure out how to exactly handle these kinds of contributions.
It is also planned to move many components to a more permissive license in the future.

## Development

Any contribution is welcomed!
There are no real formal contribution guidelines right now, they will maybe come later.

### Modularity

All X-Pipe components target [JDK 19](https://openjdk.java.net/projects/jdk/19/) and make full use of the Java Module System (JPMS).
All components are modularized, including all their dependencies.
In case a dependency is (sadly) not modularized yet, module information is manually added using [moditect](https://github.com/moditect/moditect-gradle-plugin).
Further, note that as this is a pretty complicated Java project that fully utilizes modularity,
many IDEs still have problems building this project properly.
For example, you can't build this project in eclipse or vscode as it will complain about missing modules.
The tested and recommended IDE is intellij.

### Building and Running

You can use the gradle wrapper to build and run the project:
- `gradlew app:run` will run the desktop application. You can set various useful properties in `app/build.gradle`
- `gradlew dist` will create a distributable production version in `dist/build/dist/base`.
  To include this CLI executable in this build, make sure to run `gradlew buildCli` first
- You can also run the CLI application in development mode with something like `gradlew :cli:clean :cli:run --args="daemon start"`.
  Note here that you should always clean the CLI project first, as the native image plugin is a little buggy in that regard.
- `gradlew <project>:test` will run the tests of the specified project.

You are also able to properly debug the built production application through two different methods:
- The `app/scripts/xpiped_debug` script will launch the application in debug mode and with a console attached to it
- The `app/scripts/xpiped_debug_attach` script attaches a debugger with the help of [AttachMe](https://plugins.jetbrains.com/plugin/13263-attachme).
  Just make sure that the attachme process is running within IntelliJ, and the debugger should launch automatically once you start up the application.

Note that when any unit test is run using a debugger, the X-Pipe daemon process that is started will also attempt
to connect to that debugger through [AttachMe](https://plugins.jetbrains.com/plugin/13263-attachme) as well.

### Development FAQ

##### Why are there no GitHub actions workflows or other continuous integration pipelines set up for this repository?

There are several test workflows run in a private environment as they use private test connections
such as remote server connections and database connections.
Other private workflows are responsible for packaging, signing, and distributing the releases.
So you can assume that the code is tested!

## Community

There are several ways to reach out in case you encounter any issues or questions:

- The [X-Pipe Discord Server](https://discord.gg/8y89vS8cRb>)
- The [X-Pipe Slack Server](https://join.slack.com/t/x-pipe/shared_invite/zt-1awjq0t5j-5i4UjNJfNe1VN4b_auu6Cg>)
