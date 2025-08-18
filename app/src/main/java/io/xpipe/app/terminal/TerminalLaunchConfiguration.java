package io.xpipe.app.terminal;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.LicenseRequiredException;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;
import io.xpipe.core.XPipeInstallation;

import lombok.*;
import lombok.experimental.NonFinal;

import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Value
@RequiredArgsConstructor
@AllArgsConstructor
public class TerminalLaunchConfiguration {
    DataStoreColor color;
    String coloredTitle;
    String cleanTitle;
    boolean preferTabs;
    String scriptContent;
    ShellDialect scriptDialect;

    @NonFinal
    FilePath scriptFile = null;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());

    public static TerminalLaunchConfiguration create(
            UUID request,
            DataStoreEntry entry,
            String cleanTitle,
            String adjustedTitle,
            boolean preferTabs,
            boolean alwaysPromptRestart)
            throws Exception {
        var color = entry != null ? DataStorage.get().getEffectiveColor(entry) : null;

        if (!AppPrefs.get().enableTerminalLogging().get()) {
            var d = ProcessControlProvider.get().getEffectiveLocalDialect();
            var launcherScript = d.terminalLauncherScript(request, adjustedTitle, alwaysPromptRestart);
            var config = new TerminalLaunchConfiguration(
                    entry != null ? color : null, adjustedTitle, cleanTitle, preferTabs, launcherScript, d);
            return config;
        }

        var feature = LicenseProvider.get().getFeature("logging");
        var supported = feature.isSupported();
        if (!supported) {
            throw new LicenseRequiredException(feature);
        }

        var logDir = AppProperties.get().getDataDir().resolve("sessions");
        Files.createDirectories(logDir);
        var logName = OsFileSystem.ofLocal()
                .makeFileSystemCompatible(FilePath.of(DataStorage.get().getStoreEntryDisplayName(entry) + " ("
                        + DATE_FORMATTER.format(Instant.now()) + ").log"))
                .toString()
                .replaceAll(" ", "_");

        var sc = TerminalProxyManager.getProxy().orElse(LocalShell.getShell()).start();
        var logFile = sc.getLocalSystemAccess().translateFromLocalSystemPath(FilePath.of(logDir.resolve(logName)));

        if (sc.getOsType() == OsType.WINDOWS) {
            var launcherScript = ScriptHelper.createExecScript(
                    ShellDialects.POWERSHELL,
                    sc,
                    ShellDialects.POWERSHELL.terminalLauncherScript(request, adjustedTitle, alwaysPromptRestart));
            var content =
                    """
                          echo 'Transcript started, output file is "sessions\\%s"'
                          Start-Transcript -Force -LiteralPath "%s" > $Out-Null
                          & %s
                          Stop-Transcript > $Out-Null
                          echo 'Transcript stopped, output file is "sessions\\%s"'
                          """
                            .formatted(logFile.getFileName(), logFile, launcherScript, logFile.getFileName());
            var config = new TerminalLaunchConfiguration(
                    entry != null ? color : null,
                    adjustedTitle,
                    cleanTitle,
                    preferTabs,
                    content,
                    ShellDialects.POWERSHELL);
            return config;
        } else {
            var found =
                    sc.command(sc.getShellDialect().getWhichCommand("script")).executeAndCheck();
            if (!found) {
                var suffix = sc.getOsType() == OsType.MACOS
                        ? "This command is available in the util-linux package which can be installed via homebrew."
                        : "This command is available in the util-linux package.";
                throw ErrorEventFactory.expected(
                        new IllegalStateException("Logging requires the script command to be installed. " + suffix));
            }

            var launcherScript = ScriptHelper.createExecScript(
                    LocalShell.getShell(),
                    LocalShell.getShell()
                            .getShellDialect()
                            .terminalLauncherScript(request, adjustedTitle, alwaysPromptRestart));
            var command = sc == LocalShell.getShell()
                    ? launcherScript
                    : LocalShell.getShell()
                            .getShellDialect()
                            .getOpenScriptCommand(launcherScript.toString())
                            .buildFull(LocalShell.getShell());
            var cliExecutable = TerminalProxyManager.getProxy()
                    .orElse(LocalShell.getShell())
                    .getLocalSystemAccess()
                    .translateFromLocalSystemPath(FilePath.of(XPipeInstallation.getLocalDefaultCliExecutable()));
            var scriptCommand = sc.getOsType() == OsType.MACOS || sc.getOsType() == OsType.BSD
                    ? "script -e -q '%s' \"%s\"".formatted(logFile, command)
                    : "script --quiet --command '%s' \"%s\"".formatted(command, logFile);
            var content =
                    """
                   echo "Transcript started, output file is sessions/%s"
                   %s
                   echo "Transcript stopped, output file is sessions/%s"
                   cat "%s" | "%s" terminal-clean > "%s.txt"
                   """
                            .formatted(
                                    logFile.getFileName(),
                                    scriptCommand,
                                    logFile.getFileName(),
                                    logFile,
                                    cliExecutable,
                                    logFile.getBaseName());
            var config = new TerminalLaunchConfiguration(
                    entry != null ? color : null, adjustedTitle, cleanTitle, preferTabs, content, sc.getShellDialect());
            config.scriptFile = ScriptHelper.createExecScript(sc.getShellDialect(), sc, content);
            return config;
        }
    }

    public TerminalLaunchConfiguration withScript(ShellDialect d, String content) {
        return new TerminalLaunchConfiguration(color, coloredTitle, cleanTitle, preferTabs, content, d);
    }

    @SneakyThrows
    public synchronized FilePath getScriptFile() {
        if (scriptFile == null) {
            scriptFile = ScriptHelper.createExecScript(scriptDialect, LocalShell.getShell(), scriptContent);
        }
        return scriptFile;
    }

    public synchronized CommandBuilder getDialectLaunchCommand() {
        var open = scriptDialect.getOpenScriptCommand(getScriptFile().toString());
        return open;
    }
}
