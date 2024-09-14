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

## Development Setup

You need to have an up-to-date version of XPipe installed on your local system in order to properly
run XPipe in a development environment.
This is due to the fact that some components are only included in the release version and not in this repository.
XPipe is able to automatically detect your local installation and fetch the required
components from it when it is run in a development environment.

Note that in case the current master branch is ahead of the latest release, it might happen that there are some incompatibilities when loading data from your local XPipe installation.
You should therefore always check out the matching version tag for your local repository and local XPipe installation.
You can find the available version tags at https://github.com/xpipe-io/xpipe/tags.
So for example if you currently have XPipe `10.0` installed, you should run `git reset --hard 10.0` first to properly compile against it.

You need to have JDK for Java 21 installed to compile the project.
If you are on Linux or macOS, you can easily accomplish that by running
```bash
curl -s "https://get.sdkman.io" | bash
. "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.1-graalce
sdk default java 21.0.1-graalce
```
.
On Windows, you have to manually install a JDK, e.g. from [Adoptium](https://adoptium.net/temurin/releases/?version=21).

You can configure a few development options in the file `app/dev.properties` which will be automatically generated when gradle is first run.

## Building and Running

You can use the gradle wrapper to build and run the project:
- `gradlew app:run` will run the desktop application. You can set various useful properties in `app/build.gradle`
- `gradlew clean dist` will create a distributable production version in `dist/build/dist/base`.
- `gradlew <project>:test` will run the tests of the specified project.

You are also able to properly debug the built production application through two different methods:
- The `dist/build/dist/base/app/scripts/xpiped_debug` script will launch the application in debug mode and with a console attached to it
- The `dist/build/dist/base/app/scripts/xpiped_debug_attach` script attaches a debugger with the help of [AttachMe](https://plugins.jetbrains.com/plugin/13263-attachme).
  Just make sure that the attachme process is running within IntelliJ, and the debugger should launch automatically once you start up the application.

Note that when any unit test is run using a debugger, the XPipe daemon process that is started will also attempt
to connect to that debugger through [AttachMe](https://plugins.jetbrains.com/plugin/13263-attachme) as well.

## Modularity and IDEs

All XPipe components target [Java 21](https://openjdk.java.net/projects/jdk/21/) and make full use of the Java Module System (JPMS).
All components are modularized, including all their dependencies.
In case a dependency is (sadly) not modularized yet, module information is manually added using [extra-java-module-info](https://github.com/gradlex-org/extra-java-module-info).
Further, note that as this is a pretty complicated Java project that fully utilizes modularity,
many IDEs still have problems building this project properly.

For example, you can't build this project in eclipse or vscode as it will complain about missing modules.
The tested and recommended IDE is IntelliJ.
When setting up the project in IntelliJ, make sure that the correct JDK (Java 21)
is selected both for the project and for gradle itself.

## Contributing guide

Especially when starting out, it might be a good idea to start with easy tasks first. Here's a selection of suitable common tasks that are very easy to implement:

### Interacting via the HTTP API

You can create clients they communicate with the XPipe daemon via its HTTP API.
To get started, see the [OpenAPI spec](/openapi.yaml).

### Implementing support for a new editor

All code for handling external editors can be found [here](https://github.com/xpipe-io/xpipe/blob/master/app/src/main/java/io/xpipe/app/prefs/ExternalEditorType.java). There you will find plenty of working examples that you can use as a base for your own implementation.

### Implementing support for a new terminal

All code for handling external terminals can be found [here](https://github.com/xpipe-io/xpipe/blob/master/app/src/main/java/io/xpipe/app/terminal/). There you will find plenty of working examples that you can use as a base for your own implementation.

### Adding more context menu actions in the file browser

In case you want to implement your own actions for certain file types in the file browser, you can easily do so. You can find most existing actions [here](https://github.com/xpipe-io/xpipe/tree/master/ext/base/src/main/java/io/xpipe/ext/base/browser) to get some inspiration.
Once you created your custom classes, you have to register them in your module info, just like [here](https://github.com/xpipe-io/xpipe/blob/master/ext/base/src/main/java/module-info.java).

### Implementing custom actions for the connection hub

All actions that you can perform for certain connections in the connection overview tab are implemented using an [Action API](https://github.com/xpipe-io/xpipe/blob/master/app/src/main/java/io/xpipe/app/ext/ActionProvider.java). You can find a sample implementation [here](https://github.com/xpipe-io/xpipe/blob/master/ext/base/src/main/java/io/xpipe/ext/base/action/SampleAction.java) and many common action implementations [here](https://github.com/xpipe-io/xpipe/tree/master/ext/base/src/main/java/io/xpipe/ext/base/action).

### Adding more predefined scripts

You can add custom script definitions [here](https://github.com/xpipe-io/xpipe/tree/master/ext/base/src/main/java/io/xpipe/ext/base/script/PredefinedScriptStore.java) and [here](https://github.com/xpipe-io/xpipe/tree/master/ext/base/src/main/resources/io/xpipe/ext/base/resources/scripts).

### Adding more file icons for specific types

You can register file types [here](https://github.com/xpipe-io/xpipe/blob/master/app/src/main/resources/io/xpipe/app/resources/file_list.txt) and add the respective icons [here](https://github.com/xpipe-io/xpipe/tree/master/app/src/main/resources/io/xpipe/app/resources/img/browser).

The existing file list and icons are taken from the [vscode-icons](https://github.com/vscode-icons/vscode-icons) project. Due to limitations in the file definition list compatibility, some file types might not be listed by their proper extension and are therefore not being applied correctly even though the images and definitions exist already.

### Implementing something else

if you want to work on something that was not listed here, you can still do so of course. You can reach out on the [Discord server](https://discord.gg/8y89vS8cRb) to discuss any development plans and get you started.

### Translations

See the [translation guide](/lang) for details.
