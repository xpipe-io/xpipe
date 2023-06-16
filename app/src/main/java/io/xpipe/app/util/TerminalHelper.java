package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.CommandControl;

import java.io.IOException;

public class TerminalHelper {

    public static void open(String title, CommandControl cc) throws Exception {
        var command = cc.prepareTerminalOpen(title);
        open(title, command);
    }

    public static void open(String title, String command) throws Exception {
        var type = AppPrefs.get().terminalType().getValue();
        if (type == null) {
            throw new IllegalStateException(AppI18n.get("noTerminalSet"));
        }

        command = ScriptHelper.createLocalExecScript(command);

        try {
            type.launch(title, command, false);
        } catch (Exception ex) {
            throw new IOException(
                    "Unable to launch terminal " + type.toTranslatedString() + ": " + ex.getMessage()
                            + ". Maybe try to use a different one in the settings.",
                    ex);
        }
    }
}
