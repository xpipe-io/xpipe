package io.xpipe.app.util;

import io.xpipe.app.prefs.AppPrefs;

public class TerminalHelper {

    public static void open(String title, String command) throws Exception {
        if (command.contains("\n")) {
            command = ScriptHelper.createLocalExecScript(command);
        }

        var type = AppPrefs.get().terminalType().getValue();
        type.launch(title, command);
    }
}
