package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalTerminalType;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.process.*;

import java.io.IOException;
import java.util.UUID;

public class TerminalLauncher {
    public static void open(String title, ProcessControl cc) throws Exception {
        open(null, title, null, cc);
    }

    public static void open(DataStoreEntry entry, String title, String directory, ProcessControl cc) throws Exception {
        var type = AppPrefs.get().terminalType().getValue();
        if (type == null) {
            throw ErrorEvent.unreportable(new IllegalStateException(AppI18n.get("noTerminalSet")));
        }

        var color = entry != null ? DataStorage.get().getRootForEntry(entry).getColor() : null;
        var prefix = entry != null && color != null && type.supportsColoredTitle()
                ? color.getEmoji() + " "
                : "";
        var cleanTitle = (title != null ? title : entry != null ? entry.getName() : "?");
        var adjustedTitle = prefix + cleanTitle;
        var terminalConfig = new TerminalInitScriptConfig(
                adjustedTitle,
                type.shouldClear() && AppPrefs.get().clearTerminalOnInit().get(),
                color != null);

        var request = UUID.randomUUID();
        var d = LocalShell.getShell().getShellDialect();
        var launcherScript = d.terminalLauncherScript(request, adjustedTitle);
        var preparationScript = ScriptHelper.createLocalExecScript(launcherScript);
        var config = new ExternalTerminalType.LaunchConfiguration(entry != null ? color : null, adjustedTitle, cleanTitle, preparationScript,
                d);
        var latch = TerminalLauncherManager.submitAsync(request,cc,terminalConfig,directory);
        try {
            type.launch(config);
            latch.await();
        } catch (Exception ex) {
            throw ErrorEvent.unreportable(ex);
        }
    }
}
