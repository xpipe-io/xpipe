package io.xpipe.app.terminal;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.FailableFunction;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class TerminalLauncher {

    public static void openDirect(
            String title, FailableFunction<ShellControl, String, Exception> command, ExternalTerminalType type)
            throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var script = ScriptHelper.constructTerminalInitFile(
                    sc.getShellDialect(),
                    sc,
                    WorkingDirectoryFunction.none(),
                    List.of(),
                    List.of(command.apply(sc)),
                    new TerminalInitScriptConfig(
                            title,
                            type.shouldClear()
                                    && AppPrefs.get().clearTerminalOnInit().get()
                                    && !AppPrefs.get().developerPrintInitFiles().get(),
                            TerminalInitFunction.none()),
                    true);
            var config = new TerminalLaunchConfiguration(null, title, title, true, script, sc.getShellDialect());
            type.launch(config);
        }
    }

    public static void open(String title, ProcessControl cc) throws Exception {
        open(null, title, null, cc, UUID.randomUUID(), true);
    }

    public static void open(String title, ProcessControl cc, UUID request) throws Exception {
        open(null, title, null, cc, request, true);
    }

    public static void open(DataStoreEntry entry, String title, FilePath directory, ProcessControl cc)
            throws Exception {
        open(entry, title, directory, cc, UUID.randomUUID(), true);
    }

    public static void open(
            DataStoreEntry entry, String title, FilePath directory, ProcessControl cc, UUID request, boolean preferTabs)
            throws Exception {
        var type = AppPrefs.get().terminalType().getValue();
        if (type == null) {
            throw ErrorEvent.expected(new IllegalStateException(AppI18n.get("noTerminalSet")));
        }

        var color = entry != null ? DataStorage.get().getEffectiveColor(entry) : null;
        var prefix = entry != null && color != null && type.supportsColoredTitle() ? color.getEmoji() + " " : "";
        var cleanTitle = (title != null ? title : entry != null ? entry.getName() : "?");
        var adjustedTitle = prefix + cleanTitle;
        var log = AppPrefs.get().enableTerminalLogging().get();
        var terminalConfig = new TerminalInitScriptConfig(
                adjustedTitle,
                !log
                        && type.shouldClear()
                        && AppPrefs.get().clearTerminalOnInit().get()
                        && !AppPrefs.get().developerPrintInitFiles().get(),
                cc instanceof ShellControl ? type.additionalInitCommands() : TerminalInitFunction.none());
        var config = TerminalLaunchConfiguration.create(request, entry, cleanTitle, adjustedTitle, preferTabs);
        var latch = TerminalLauncherManager.submitAsync(request, cc, terminalConfig, directory);
        try {
            type.launch(config);
            latch.await();
        } catch (Exception ex) {
            var modMsg = ex.getMessage() != null && ex.getMessage().contains("Unable to find application named")
                    ? ex.getMessage() + " in installed /Applications on this system"
                    : ex.getMessage();
            throw ErrorEvent.expected(new IOException(
                    "Unable to launch terminal " + type.toTranslatedString().getValue() + ": " + modMsg, ex));
        }
    }
}
