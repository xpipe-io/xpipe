package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.FailableFunction;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class TerminalLauncher {

    public static void openDirect(String title, FailableFunction<ShellControl, String, Exception> command)
            throws Exception {
        var type = AppPrefs.get().terminalType().getValue();
        if (type == null) {
            throw ErrorEvent.expected(new IllegalStateException(AppI18n.get("noTerminalSet")));
        }
        openDirect(title, command, type);
    }

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
                                    && AppPrefs.get().clearTerminalOnInit().get(),
                            TerminalInitFunction.none()),
                    true);
            var config = new ExternalTerminalType.LaunchConfiguration(null, title, title, script, sc.getShellDialect());
            type.launch(config);
        }
    }

    public static void open(String title, ProcessControl cc) throws Exception {
        open(null, title, null, cc);
    }

    public static void open(DataStoreEntry entry, String title, String directory, ProcessControl cc) throws Exception {
        var type = AppPrefs.get().terminalType().getValue();
        if (type == null) {
            throw ErrorEvent.expected(new IllegalStateException(AppI18n.get("noTerminalSet")));
        }

        var color = entry != null ? DataStorage.get().getEffectiveColor(entry) : null;
        var prefix = entry != null && color != null && type.supportsColoredTitle() ? color.getEmoji() + " " : "";
        var cleanTitle = (title != null ? title : entry != null ? entry.getName() : "?");
        var adjustedTitle = prefix + cleanTitle;
        var terminalConfig = new TerminalInitScriptConfig(
                adjustedTitle,
                type.shouldClear() && AppPrefs.get().clearTerminalOnInit().get(),
                cc instanceof ShellControl ? type.additionalInitCommands() : TerminalInitFunction.none());
        var request = UUID.randomUUID();
        var config = createConfig(request, entry, cleanTitle, adjustedTitle);
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

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());

    private static ExternalTerminalType.LaunchConfiguration createConfig(
            UUID request, DataStoreEntry entry, String cleanTitle, String adjustedTitle) throws Exception {
        var color = entry != null ? DataStorage.get().getEffectiveColor(entry) : null;
        var d = ProcessControlProvider.get().getEffectiveLocalDialect();
        var launcherScript = d.terminalLauncherScript(request, adjustedTitle);
        var preparationScript = ScriptHelper.createLocalExecScript(launcherScript);

        if (!AppPrefs.get().enableTerminalLogging().get()) {
            var config = new ExternalTerminalType.LaunchConfiguration(
                    entry != null ? color : null, adjustedTitle, cleanTitle, preparationScript, d);
            return config;
        }

        var logDir = AppProperties.get().getDataDir().resolve("sessions");
        Files.createDirectories(logDir);
        var logFile = logDir.resolve(new FilePath(DataStorage.get().getStoreEntryDisplayName(entry) + " ("
                        + DATE_FORMATTER.format(Instant.now()) + ").log")
                .fileSystemCompatible(OsType.getLocal())
                .toString());
        try (var sc = LocalShell.getShell().start()) {
            if (OsType.getLocal() == OsType.WINDOWS) {
                var content =
                        """
                              echo 'Transcript started, output file is "sessions\\%s"'
                              Start-Transcript -Force -LiteralPath "%s" > $Out-Null
                              & %s
                              Stop-Transcript > $Out-Null
                              echo 'Transcript stopped, output file is "sessions\\%s"'
                              """
                                .formatted(
                                        logFile.getFileName().toString(),
                                        logFile,
                                        preparationScript,
                                        logFile.getFileName().toString());
                var ps = ScriptHelper.createExecScript(ShellDialects.POWERSHELL, sc, content);
                var config = new ExternalTerminalType.LaunchConfiguration(
                        entry != null ? color : null, adjustedTitle, cleanTitle, ps, ShellDialects.POWERSHELL);
                return config;
            } else {
                var content =
                        """
                              script --command "%s" "%s"
                              """
                                .formatted(preparationScript, logFile);
                var ps = ScriptHelper.createExecScript(sc.getShellDialect(), sc, content);
                var config = new ExternalTerminalType.LaunchConfiguration(
                        entry != null ? color : null, adjustedTitle, cleanTitle, ps, sc.getShellDialect());
                return config;
            }
        }
    }
}
