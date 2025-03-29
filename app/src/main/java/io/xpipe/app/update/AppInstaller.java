package io.xpipe.app.update;

import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.util.FailableRunnable;
import io.xpipe.core.util.XPipeInstallation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;

import java.nio.file.Files;
import java.nio.file.Path;

public class AppInstaller {

    public static InstallerAssetType getSuitablePlatformAsset() {
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            return new InstallerAssetType.Msi();
        }

        if (OsType.getLocal().equals(OsType.LINUX)) {
            return Files.exists(Path.of("/etc/debian_version"))
                    ? new InstallerAssetType.Debian()
                    : new InstallerAssetType.Rpm();
        }

        if (OsType.getLocal().equals(OsType.MACOS)) {
            return new InstallerAssetType.Pkg();
        }

        throw new AssertionError();
    }

    @Getter
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = InstallerAssetType.Msi.class),
        @JsonSubTypes.Type(value = InstallerAssetType.Debian.class),
        @JsonSubTypes.Type(value = InstallerAssetType.Rpm.class),
        @JsonSubTypes.Type(value = InstallerAssetType.Pkg.class)
    })
    public abstract static class InstallerAssetType {

        protected void runAndClose(FailableRunnable<Exception> r) {
            OperationMode.executeAfterShutdown(() -> {
                r.run();

                // In case we perform any operations such as opening a terminal
                // give it some time to open while this process is still alive
                // Otherwise it might quit because the parent process is dead already
                ThreadHelper.sleep(100);
            });
        }

        public abstract void installLocal(Path file) throws Exception;

        public abstract String getExtension();

        @JsonTypeName("msi")
        public static final class Msi extends InstallerAssetType {

            @Override
            public void installLocal(Path file) throws Exception {
                var restartExec = (AppProperties.get().isDevelopmentEnvironment()
                                ? Path.of(XPipeInstallation.getLocalDefaultInstallationBasePath())
                                : XPipeInstallation.getCurrentInstallationBasePath())
                        .resolve(XPipeInstallation.getDaemonExecutablePath(OsType.getLocal()))
                        .toString();
                var logsDir =
                        AppLogs.get().getSessionLogsDirectory().getParent().toString();
                var logFile = FileNames.join(
                        logsDir, "installer_" + file.getFileName().toString() + ".log");
                var systemWide = isSystemWide();
                var command = LocalShell.getShell().getShellDialect().equals(ShellDialects.CMD) && !systemWide
                        ? getCmdCommand(file.toString(), logFile, restartExec)
                        : getPowershellCommand(file.toString(), logFile, restartExec, systemWide);
                String toRun;
                if (ProcessControlProvider.get().getEffectiveLocalDialect() == ShellDialects.CMD && !systemWide) {
                    toRun = "start \"XPipe Updater\" /min cmd /c \"" + ScriptHelper.createLocalExecScript(command)
                            + "\"";
                } else {
                    toRun =
                            "Start-Process -WindowStyle Minimized -FilePath powershell -ArgumentList  \"-ExecutionPolicy\", \"Bypass\", \"-File\", \"`\""
                                    + ScriptHelper.createLocalExecScript(command) + "`\"\"";
                }
                runAndClose(() -> {
                    LocalShell.getShell().executeSimpleCommand(toRun);
                });
            }

            @Override
            public String getExtension() {
                return "msi";
            }

            private boolean isSystemWide() {
                return Files.exists(
                        XPipeInstallation.getCurrentInstallationBasePath().resolve("system"));
            }

            private String getCmdCommand(String file, String logFile, String exec) {
                var args = "MSIFASTINSTALL=7 DISABLEROLLBACK=1";
                return String.format(
                        """
                        echo Installing %s ...
                        cd /D "%%HOMEDRIVE%%%%HOMEPATH%%"
                        echo + msiexec /i "%s" /lv "%s" /qb %s
                        start "" /wait msiexec /i "%s" /lv "%s" /qb %s
                        echo Starting XPipe ...
                        start "" "%s" "-Dio.xpipe.app.dataDir=%s"
                        """,
                        file,
                        file,
                        logFile,
                        args,
                        file,
                        logFile,
                        args,
                        exec,
                        AppProperties.get().getDataDir());
            }

            private String getPowershellCommand(String file, String logFile, String exec, boolean systemWide) {
                var property = "MSIFASTINSTALL=7 DISABLEROLLBACK=1" + (systemWide ? " ALLUSERS=1" : "");
                var startProcessProperty = ", MSIFASTINSTALL=7, DISABLEROLLBACK=1" + (systemWide ? ", ALLUSERS=1" : "");
                var runas = systemWide ? "-Verb runAs" : "";
                return String.format(
                        """
                        echo Installing %s ...
                        cd "$env:HOMEDRIVE\\$env:HOMEPATH"
                        echo '+ msiexec /i "%s" /lv "%s" /qb%s'
                        Start-Process %s -FilePath msiexec -Wait -ArgumentList "/i", "`"%s`"", "/lv", "`"%s`"", "/qb"%s
                        echo 'Starting XPipe ...'
                        & "%s" "-Dio.xpipe.app.dataDir=%s"
                        """,
                        file,
                        file,
                        logFile,
                        property,
                        runas,
                        file,
                        logFile,
                        startProcessProperty,
                        exec,
                        AppProperties.get().getDataDir());
            }
        }

        @JsonTypeName("debian")
        public static final class Debian extends InstallerAssetType {

            @Override
            public void installLocal(Path file) {
                var start = AppPrefs.get() != null
                        && AppPrefs.get().terminalType().getValue() != null
                        && AppPrefs.get().terminalType().getValue().isAvailable();
                if (!start) {
                    return;
                }

                var name = AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe";
                var command = String.format(
                        """
                                             runinstaller() {
                                                 echo "Installing downloaded .deb installer ..."
                                                 echo "+ sudo apt install \\"%s\\""
                                                 DEBIAN_FRONTEND=noninteractive sudo apt-get install -qy "%s" || return 1
                                                 %s open -d "%s" || return 1
                                             }

                                             cd ~
                                             runinstaller || echo "Update failed ..." && read key
                                             """,
                        file, file, name, AppProperties.get().getDataDir());

                runAndClose(() -> {
                    // We can't use the SSH bridge
                    var type = ExternalTerminalType.determineFallbackTerminalToOpen(
                            AppPrefs.get().terminalType().getValue());
                    TerminalLauncher.openDirect("XPipe Updater", sc -> command, type);
                });
            }

            @Override
            public String getExtension() {
                return "deb";
            }
        }

        @JsonTypeName("rpm")
        public static final class Rpm extends InstallerAssetType {

            @Override
            public void installLocal(Path file) {
                var start = AppPrefs.get() != null
                        && AppPrefs.get().terminalType().getValue() != null
                        && AppPrefs.get().terminalType().getValue().isAvailable();
                if (!start) {
                    return;
                }

                var name = AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe";
                var command = String.format(
                        """
                                             runinstaller() {
                                                 echo "Installing downloaded .rpm installer ..."
                                                 echo "+ sudo rpm -U -v --force \\"%s\\""
                                                 sudo rpm -U -v --force "%s" || return 1
                                                 %s open -d "%s" || return 1
                                             }

                                             cd ~
                                             runinstaller || read -rsp "Update failed ..."$'\\n' -n 1 key
                                             """,
                        file, file, name, AppProperties.get().getDataDir());

                runAndClose(() -> {
                    // We can't use the SSH bridge
                    var type = ExternalTerminalType.determineFallbackTerminalToOpen(
                            AppPrefs.get().terminalType().getValue());
                    TerminalLauncher.openDirect("XPipe Updater", sc -> command, type);
                });
            }

            @Override
            public String getExtension() {
                return "rpm";
            }
        }

        @JsonTypeName("pkg")
        public static final class Pkg extends InstallerAssetType {

            @Override
            public void installLocal(Path file) {
                var start = AppPrefs.get() != null
                        && AppPrefs.get().terminalType().getValue() != null
                        && AppPrefs.get().terminalType().getValue().isAvailable();
                if (!start) {
                    return;
                }

                var name = AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe";
                var command = String.format(
                        """
                                           runinstaller() {
                                               echo "Installing downloaded .pkg installer ..."
                                               echo "+ sudo installer -verboseR -pkg \\"%s\\" -target /"
                                               sudo installer -verboseR -pkg "%s" -target / || return 1
                                               %s open -d "%s" || return 1
                                           }

                                           cd ~
                                           runinstaller || echo "Update failed ..." && read -rs -k 1 key
                                           """,
                        file, file, name, AppProperties.get().getDataDir());

                runAndClose(() -> {
                    // We can't use the SSH bridge
                    var type = ExternalTerminalType.determineFallbackTerminalToOpen(
                            AppPrefs.get().terminalType().getValue());
                    TerminalLauncher.openDirect("XPipe Updater", sc -> command, type);
                });
            }

            @Override
            public String getExtension() {
                return "pkg";
            }
        }
    }
}
