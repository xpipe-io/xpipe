package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.CommandControl;

public class TerminalHelper {

    public static void open(String title, CommandControl cc) throws Exception {
        var command = cc.prepareTerminalOpen();
        open(title, command);
    }

    public static void open(String title, String command) throws Exception {
        if (command.contains("\n") || command.contains(" ")) {
            command = ScriptHelper.createLocalExecScript(command);
        }

        var type = AppPrefs.get().terminalType().getValue();
        if (type == null) {
            throw new IllegalStateException(AppI18n.get("noTerminalSet"));
        }

        type.launch(title, command);
    }
}
