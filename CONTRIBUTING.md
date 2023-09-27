# Contributors guide

If you're interested in contributing to XPipe, you can easily do so! Just submit a pull request with your changes.

In terms of development environment setup, be sure to read the [development page](https://github.com/xpipe-io/xpipe/blob/master/DEVELOPMENT.md) first.
Especially when starting out, it might be a good idea to start with easy tasks first. Here's a selection of suitable common tasks that are very easy to implement:

### Implementing support for a new editor

All code for handling external editors can be found [here](https://github.com/xpipe-io/xpipe/blob/master/app/src/main/java/io/xpipe/app/prefs/ExternalEditorType.java). There you will find plenty of working examples that you can use as a base for your own implementation.

### Implementing support for a new terminal

All code for handling external terminals can be found [here](https://github.com/xpipe-io/xpipe/blob/master/app/src/main/java/io/xpipe/app/prefs/ExternalTerminalType.java). There you will find plenty of working examples that you can use as a base for your own implementation.

### Adding more file icons for specific types

You can register file types [here](https://github.com/xpipe-io/xpipe/blob/master/app/src/main/resources/io/xpipe/app/resources/file_list.txt) and add the respective icons [here](https://github.com/xpipe-io/xpipe/tree/master/app/src/main/resources/io/xpipe/app/resources/browser_icons).

The existing file list and icons are taken from the [vscode-icons](https://github.com/vscode-icons/vscode-icons) project. Due to limitations in the file definition list compatibility, some file types might not be listed by their proper extension and are therefore not being applied correctly even though the images and definitions exist already.

### Adding more context menu actions in the file browser

In case you want to implement your own actions for certain file types in the file browser, you can easily do so. You can find most existing actions [here](https://github.com/xpipe-io/xpipe/tree/master/ext/base/src/main/java/io/xpipe/ext/base/browser) to get some inspiration.
Once you created your custom classes, you have to register them in your module info, just like [here](https://github.com/xpipe-io/xpipe/blob/master/ext/base/src/main/java/module-info.java).

### Implementing custom actions for the connection hub

All actions that you can perform for certain connections in the connection overview tab are implemented using an [Action API](https://github.com/xpipe-io/xpipe/blob/master/app/src/main/java/io/xpipe/app/ext/ActionProvider.java). You can find a sample implementation [here](https://github.com/xpipe-io/xpipe/blob/master/ext/base/src/main/java/io/xpipe/ext/base/action/SampleAction.java) and many common action implementations [here](https://github.com/xpipe-io/xpipe/tree/master/ext/base/src/main/java/io/xpipe/ext/base/action).

### Familiarising yourself with the shell and command API

The [sample action](https://github.com/xpipe-io/xpipe/blob/master/ext/base/src/main/java/io/xpipe/ext/base/action/SampleAction.java) shows the basics of working with shells and executing commands in them. For more references, just look for the usages of the [API classes](https://github.com/xpipe-io/xpipe/tree/master/core/src/main/java/io/xpipe/core/process).

### Implementing something else

if you want to work on something that was not listed here, you can still do so of course. You can reach out on the [Discord server](https://discord.gg/8y89vS8cRb) to discuss any development plans and get you started.
