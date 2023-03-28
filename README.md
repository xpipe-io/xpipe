<img src="https://user-images.githubusercontent.com/72509152/213873342-7638e830-8a95-4b5d-ad3e-5a9a0b4bf538.png" alt="drawing" width="300"/>

### A smart connection manager and remote file explorer

X-Pipe is a brand new type of connection manager and remote file explorer that works by exclusively interacting with CLI tools on local and remote shell connections.
This approach makes it much more flexible as it doesn't have to deal with file system APIs or remote file handling protocols at all.

X-Pipe integrates with your existing tools and workflows
by outsourcing as much tasks as possible to your
text/code editors, terminals, shells, command-line tools and more.
The platform is open source and designed to be extensible, allowing anyone
to implement custom functionality through custom extensions.

## Getting Started

Head over to the [releases page](https://github.com/xpipe-io/xpipe/releases) and try it out.
Feel free to file tickets for bugs or feature requests. Upvote 👍 the ones you'd like to see prioritized.
There are several ways to reach out in case you encounter any issues or questions:
- The [X-Pipe Discord Server](https://discord.gg/8y89vS8cRb)
- The [X-Pipe Slack Server](https://join.slack.com/t/x-pipe/shared_invite/zt-1awjq0t5j-5i4UjNJfNe1VN4b_auu6Cg)

## Features

### Flexible remote connector

- Can connect to standard servers, database servers, and more
- Supports established protocols (e.g. SSH and more) plus any custom connection methods that work through the command-line
- Is able to integrate any kind of proxies into the connection process, even ones with different protocols

### Instant launch for remote shells and commands

- Automatically login into a shell in your favourite terminal with one click (no need to fill password prompts, etc.)
- Works for all kinds of shells. This includes command shells (e.g. bash, PowerShell, cmd, etc.) and database shells (e.g. PostgreSQL Shell)
- Comes with integrations for all commonly used terminals for all operating systems
- Allows you to customize the launched shell's init environment
- Supports launches from the GUI or command-line

### Manage all your connections in one place

- Easily create and manage all kinds of remote connections at one location
- Securely stores all information exclusively on your computer and encrypts all secret information
- Share connection configurations to any other trusted party through shareable URLs
- Create desktop shortcuts for your connections

<img src="https://user-images.githubusercontent.com/72509152/221438232-18bc1ee3-4b3f-49dc-8fb0-df8896aa7c91.png" alt="drawing" height="450"/>

### Remote file explorer

- Access files on any remote system
- Quickly open a terminal into any directory
- Run commands from the explorer interface
- Utilize your favourite local programs to open and edit remote files

<img src="https://user-images.githubusercontent.com/72509152/221438071-e3f287ea-a6ee-49b3-a776-4b5b39b1fd92.png" alt="drawing" height="450"/>

## Repository Structure

The following for modules make up the X-Pipe API and a licensed under the MIT license:
- [core](core) - Shared core classes of the X-Pipe Java API, X-Pipe extensions, and the X-Pipe daemon implementation
- [beacon](beacon) - The X-Pipe beacon component is responsible for handling all communications between the X-Pipe daemon
  and the client applications, for example the various programming language APIs and the CLI

The other modules make up the X-Pipe implementation and are licensed under GPL:
- [app](app) - Contains the X-Pipe daemon implementation, the X-Pipe desktop application, and an
  API to create all different kinds of extensions for the X-Pipe platform
- [dist](dist) - Tools to create a distributable package of X-Pipe
- [ext](ext) - Available X-Pipe extensions. Essentially every feature is implemented as an extension

### Open source model

X-Pipe utilizes an open core model, which essentially means that
the main application is open source while certain other components are not.
Some tests and especially test environments and that run on private servers
are also not included in this repository (Don't want to leak server information).
Finally, scripts and workflows to create signed executables and installers
are also not included to prevent attackers from easily impersonating the shipping the X-Pipe application malware.

The license model is chosen in such a way that you are
able to use and integrate X-Pipe within your application through the MIT-licensed API.

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

### Setup

You need to have an up-to-date version of X-Pipe installed on your local system in order to properly
run X-Pipe in a development environment.
This is due to the fact that some components are only included in the release version and not in this repository.
X-Pipe is able to automatically detect your installation and fetch the required
components from it when it is run in a development environment.

### Building and Running

You can use the gradle wrapper to build and run the project:
- `gradlew app:run` will run the desktop application. You can set various useful properties in `app/build.gradle`
- `gradlew dist` will create a distributable production version in `dist/build/dist/base`.
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
