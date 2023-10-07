package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalTerminalType;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.process.ProcessControl;

import java.io.IOException;

public class TerminalHelper {

    public static void open(String title, ProcessControl cc) throws Exception {
        var command = cc.prepareTerminalOpen(title);
        open(null, title, cc);
    }

    public static void open(DataStoreEntry entry, String title, ProcessControl cc) throws Exception {
        var type = AppPrefs.get().terminalType().getValue();
        if (type == null) {
            throw ErrorEvent.unreportable(new IllegalStateException(AppI18n.get("noTerminalSet")));
        }

        var prefix = entry != null && entry.getColor() != null && type.supportsColoredTitle()
                ? entry.getColor().getEmoji() + " "
                : "";
        var fixedTitle = prefix + (title != null ? title : entry != null ? entry.getName() : "?");
        var file = ScriptHelper.createLocalExecScript(cc.prepareTerminalOpen(fixedTitle));
        var config = new ExternalTerminalType.LaunchConfiguration(entry != null ? entry.getColor() : null, title, file);
        try {
            type.launch(config);
        } catch (Exception ex) {
            throw ErrorEvent.unreportable(new IOException(
                    "Unable to launch terminal " + type.toTranslatedString() + ": " + ex.getMessage()
                            + ".\nMaybe try to use a different terminal in the settings.",
                    ex));
        }
    }
}
