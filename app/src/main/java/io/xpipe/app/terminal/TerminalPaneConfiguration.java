package io.xpipe.app.terminal;

import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.*;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.LicenseRequiredException;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Value
@RequiredArgsConstructor
@AllArgsConstructor
public class TerminalPaneConfiguration {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());

    UUID request;
    String title;
    int paneIndex;
    String scriptContent;
    ShellDialect scriptDialect;
    @NonFinal
    FilePath scriptFile = null;

    private static Path getLogFile(int index, DataStoreEntry entry) throws Exception {
        var logDir = AppProperties.get().getDataDir().resolve("sessions");
        Files.createDirectories(logDir);
        var suffix = index > 0 ? "-" + (index + 1) : "";
        var name = DataStorage.get().getStoreEntryDisplayName(entry) + "_" + DATE_FORMATTER.format(Instant.now()) + suffix + ".log";
        var logName = OsFileSystem.ofLocal()
                .makeFileSystemCompatible(FilePath.of(name))
                .toString()
                .replaceAll(" ", "_");
        return logDir.resolve(logName);
    }

    public static TerminalPaneConfiguration create(
            UUID request,
            DataStoreEntry entry,
            String title,
            int paneIndex,
            boolean enableLogging,
            boolean alwaysPromptRestart)
            throws Exception {
        if (!enableLogging || !AppPrefs.get().enableTerminalLogging().get()) {
            var d = LocalShell.getDialect();
            var register = getTerminalRegisterCommand(request);
            var powershell = ShellDialects.isPowershell(d);
            var launcherScript = (powershell ? "& " + register : register) + "\n" + d.terminalLauncherScript(request, title, alwaysPromptRestart);
            var config = new TerminalPaneConfiguration(request, title, paneIndex, launcherScript, d);
            return config;
        }

        var feature = LicenseProvider.get().getFeature("logging");
        var supported = feature.isSupported();
        if (!supported) {
            throw new LicenseRequiredException(feature);
        }

        var log = getLogFile(paneIndex, entry);
        var sc = TerminalProxyManager.getProxy().orElse(LocalShell.getShell()).start();
        var logFile = sc.getLocalSystemAccess().translateFromLocalSystemPath(FilePath.of(log));

        if (sc.getOsType() == OsType.WINDOWS) {
            var launcherScript = ScriptHelper.createExecScript(
                    ShellDialects.POWERSHELL,
                    sc,
                    ShellDialects.POWERSHELL.terminalLauncherScript(request, title, alwaysPromptRestart));
            var content =
                    """
                          & %s
                          echo 'Transcript started, output file is "sessions\\%s"'
                          Start-Transcript -Force -LiteralPath "%s" > $Out-Null
                          & "%s"
                          Stop-Transcript > $Out-Null
                          echo 'Transcript stopped, output file is "sessions\\%s"'
                          """
                            .formatted(getTerminalRegisterCommand(request), logFile.getFileName(), logFile, launcherScript, logFile.getFileName());
            var config = new TerminalPaneConfiguration(
                    request,
                    title,
                    paneIndex,
                    content,
                    ShellDialects.POWERSHELL);
            return config;
        } else {
            var found = sc.view().findProgram("script").isPresent();
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
                            .terminalLauncherScript(request, title, alwaysPromptRestart));
            var command = sc == LocalShell.getShell()
                    ? launcherScript
                    : LocalShell.getShell()
                            .getShellDialect()
                            .getOpenScriptCommand(launcherScript.toString())
                            .buildFull(LocalShell.getShell());
            var cliExecutable = TerminalProxyManager.getProxy()
                    .orElse(LocalShell.getShell())
                    .getLocalSystemAccess()
                    .translateFromLocalSystemPath(
                            FilePath.of(AppInstallation.ofCurrent().getCliExecutablePath()));
            var scriptCommand = sc.getOsType() == OsType.MACOS || sc.getOsType() == OsType.BSD
                    ? "script -e -q '%s' \"%s\"".formatted(logFile, command)
                    : "script --quiet --command '%s' \"%s\"".formatted(command, logFile);
            var content =
                    """
                          %s
                          echo "Transcript started, output file is sessions/%s"
                          %s
                          echo "Transcript stopped, output file is sessions/%s"
                          cat "%s" | "%s" terminal-clean > "%s.txt"
                          """
                            .formatted(
                                    getTerminalRegisterCommand(request),
                                    logFile.getFileName(),
                                    scriptCommand,
                                    logFile.getFileName(),
                                    logFile,
                                    cliExecutable,
                                    logFile.getBaseName());
            var config = new TerminalPaneConfiguration(request, title, paneIndex, content, sc.getShellDialect());
            config.scriptFile = ScriptHelper.createExecScript(sc.getShellDialect(), sc, content);
            return config;
        }
    }

    private static String getTerminalRegisterCommand(UUID request) throws Exception {
        var exec = AppInstallation.ofCurrent().getCliExecutablePath();
        var registerLine = CommandBuilder.of()
                .addFile(exec)
                .add("terminal-register", "--request", request.toString())
                .buildSimple();
        var bellLine = "printf \"\\a\"";
        var printBell = OsType.ofLocal() != OsType.WINDOWS
                && AppPrefs.get().enableTerminalStartupBell().get();
        var lines = ShellScript.lines(registerLine, printBell ? bellLine : null);
        return lines.toString();
    }

    public TerminalPaneConfiguration withScript(ShellDialect d, String content) {
        return new TerminalPaneConfiguration(request, title, paneIndex, content, d);
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
