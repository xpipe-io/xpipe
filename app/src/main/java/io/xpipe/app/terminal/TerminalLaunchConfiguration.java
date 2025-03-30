package io.xpipe.app.terminal;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataColor;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.LicenseRequiredException;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FilePath;

import lombok.Value;
import lombok.With;

import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Value
@With
public class TerminalLaunchConfiguration {
    DataColor color;
    String coloredTitle;
    String cleanTitle;
    boolean preferTabs;

    FilePath scriptFile;

    ShellDialect scriptDialect;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());

    public static TerminalLaunchConfiguration create(
            UUID request,
            DataStoreEntry entry,
            String cleanTitle,
            String adjustedTitle,
            boolean preferTabs,
            boolean promptRestart)
            throws Exception {
        var color = entry != null ? DataStorage.get().getEffectiveColor(entry) : null;
        var d = ProcessControlProvider.get().getEffectiveLocalDialect();
        var launcherScript = d.terminalLauncherScript(request, adjustedTitle, promptRestart);
        var preparationScript = ScriptHelper.createLocalExecScript(launcherScript);

        if (!AppPrefs.get().enableTerminalLogging().get()) {
            var config = new TerminalLaunchConfiguration(
                    entry != null ? color : null, adjustedTitle, cleanTitle, preferTabs, preparationScript, d);
            return config;
        }

        var feature = LicenseProvider.get().getFeature("logging");
        var supported = feature.isSupported();
        if (!supported) {
            throw new LicenseRequiredException(feature);
        }

        var logDir = AppProperties.get().getDataDir().resolve("sessions");
        Files.createDirectories(logDir);
        var logFile = logDir.resolve(FilePath.of(DataStorage.get().getStoreEntryDisplayName(entry) + " ("
                        + DATE_FORMATTER.format(Instant.now()) + ").log")
                .fileSystemCompatible(OsType.getLocal())
                .toString()
                .replaceAll(" ", "_"));
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
                var config = new TerminalLaunchConfiguration(
                        entry != null ? color : null,
                        adjustedTitle,
                        cleanTitle,
                        preferTabs,
                        ps,
                        ShellDialects.POWERSHELL);
                return config;
            } else {
                var found = sc.command(sc.getShellDialect().getWhichCommand("script"))
                        .executeAndCheck();
                if (!found) {
                    var suffix = sc.getOsType() == OsType.MACOS
                            ? "This command is available in the util-linux package which can be installed via homebrew."
                            : "This command is available in the util-linux package.";
                    throw ErrorEvent.expected(new IllegalStateException(
                            "Logging requires the script command to be installed. " + suffix));
                }

                var content = sc.getOsType() == OsType.MACOS || sc.getOsType() == OsType.BSD
                        ? """
                       echo "Transcript started, output file is sessions/%s"
                       script -e -q "%s" "%s"
                       echo "Transcript stopped, output file is sessions/%s"
                       """
                                .formatted(logFile.getFileName(), logFile, preparationScript, logFile.getFileName())
                        : """
                       echo "Transcript started, output file is sessions/%s"
                       script --quiet --command "%s" "%s"
                       echo "Transcript stopped, output file is sessions/%s"
                       """
                                .formatted(logFile.getFileName(), preparationScript, logFile, logFile.getFileName());
                var ps = ScriptHelper.createExecScript(sc.getShellDialect(), sc, content);
                var config = new TerminalLaunchConfiguration(
                        entry != null ? color : null, adjustedTitle, cleanTitle, preferTabs, ps, sc.getShellDialect());
                return config;
            }
        }
    }

    public CommandBuilder getDialectLaunchCommand() {
        var open = scriptDialect.getOpenScriptCommand(scriptFile.toString());
        return open;
    }
}
