package io.xpipe.app.update;

import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.AppRestart;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;

import java.nio.file.Files;
import java.nio.file.Path;

public class AppInstaller {

    public static InstallerAssetType getSuitablePlatformAsset() {
        if (OsType.getLocal() == OsType.WINDOWS) {
            return new InstallerAssetType.Msi();
        }

        if (OsType.getLocal() == OsType.LINUX) {
            return Files.exists(Path.of("/etc/debian_version"))
                    ? new InstallerAssetType.Debian()
                    : new InstallerAssetType.Rpm();
        }

        if (OsType.getLocal() == OsType.MACOS) {
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

        public abstract void installLocal(Path file);

        public abstract String getExtension();

        @JsonTypeName("msi")
        public static final class Msi extends InstallerAssetType {

            @Override
            public void installLocal(Path file) {
                var logsDir =
                        AppLogs.get().getSessionLogsDirectory().getParent().toString();
                var logFile =
                        FilePath.of(logsDir, "installer_" + file.getFileName().toString() + ".log");
                var systemWide = isSystemWide();
                var cmdScript = LocalShell.getDialect() == ShellDialects.CMD && !systemWide;
                var command = cmdScript
                        ? getCmdCommand(file.toString(), logFile.toString())
                        : getPowershellCommand(file.toString(), logFile.toString(), systemWide);

                OperationMode.executeAfterShutdown(() -> {
                    try (var sc = LocalShell.getShell().start()) {
                        String toRun;
                        if (cmdScript) {
                            toRun = "start \"XPipe Updater\" /min cmd /c \""
                                    + ScriptHelper.createExecScript(ShellDialects.CMD, sc, command) + "\"";
                        } else {
                            toRun = sc.getShellDialect() == ShellDialects.POWERSHELL
                                    ? "Start-Process -WindowStyle Minimized -FilePath powershell -ArgumentList  \"-ExecutionPolicy\", \"Bypass\", \"-File\", \"`\""
                                            + ScriptHelper.createExecScript(ShellDialects.POWERSHELL, sc, command)
                                            + "`\"\""
                                    : "start \"XPipe Updater\" /min powershell -ExecutionPolicy Bypass -File \""
                                            + ScriptHelper.createExecScript(ShellDialects.POWERSHELL, sc, command)
                                            + "\"";
                        }
                        sc.command(toRun).execute();
                    }
                });
            }

            @Override
            public String getExtension() {
                return "msi";
            }

            private boolean isSystemWide() {
                return Files.exists(
                        AppInstallation.ofCurrent().getBaseInstallationPath().resolve("system"));
            }

            private String getCmdCommand(String file, String logFile) {
                var args = "MSIFASTINSTALL=7 DISABLEROLLBACK=1";
                return String.format(
                        """
                        echo Installing %s ...
                        cd /D "%%HOMEDRIVE%%%%HOMEPATH%%"
                        echo + msiexec /i "%s" /lv "%s" /qb %s
                        start "" /wait msiexec /i "%s" /lv "%s" /qb %s
                        %s
                        """,
                        file,
                        file,
                        logFile,
                        args,
                        file,
                        logFile,
                        args,
                        AppRestart.getBackgroundRestartCommand(ShellDialects.CMD));
            }

            private String getPowershellCommand(String file, String logFile, boolean systemWide) {
                var property = "MSIFASTINSTALL=7 DISABLEROLLBACK=1" + (systemWide ? " ALLUSERS=1" : "");
                var startProcessProperty = ", MSIFASTINSTALL=7, DISABLEROLLBACK=1" + (systemWide ? ", ALLUSERS=1" : "");
                var runas = systemWide ? "-Verb runAs" : "";
                return String.format(
                        """
                        echo Installing %s ...
                        cd "$env:HOMEDRIVE\\$env:HOMEPATH"
                        echo '+ msiexec /i "%s" /lv "%s" /qb%s'
                        Start-Process %s -FilePath msiexec -Wait -ArgumentList "/i", "`"%s`"", "/lv", "`"%s`"", "/qb"%s
                        %s
                        """,
                        file,
                        file,
                        logFile,
                        property,
                        runas,
                        file,
                        logFile,
                        startProcessProperty,
                        AppRestart.getBackgroundRestartCommand(ShellDialects.POWERSHELL));
            }
        }

        @JsonTypeName("debian")
        public static final class Debian extends InstallerAssetType {

            @Override
            public void installLocal(Path file) {
                var command = new ShellScript(String.format(
                        """
                                             runinstaller() {
                                                 echo "Installing downloaded .deb installer ..."
                                                 echo "+ sudo apt install \\"%s\\""
                                                 DEBIAN_FRONTEND=noninteractive sudo apt install -y "%s" || return 1
                                                 %s || return 1
                                             }

                                             cd ~
                                             runinstaller
                                             if [ "$?" != 0 ]; then
                                               echo "Update failed ..."
                                               read key
                                             fi
                                             """,
                        file, file, AppRestart.getTerminalRestartCommand()));
                OperationMode.executeAfterShutdown(() -> {
                    TerminalLaunch.builder()
                            .title("XPipe Updater")
                            .localScript(command)
                            .launch();
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
                var command = new ShellScript(String.format(
                        """
                                             runinstaller() {
                                                 echo "Installing downloaded .rpm installer ..."
                                                 echo "+ sudo rpm -U -v --force \\"%s\\""
                                                 sudo rpm -U -v --force "%s" || return 1
                                                 %s || return 1
                                             }

                                             cd ~
                                             runinstaller
                                             if [ "$?" != 0 ]; then
                                               echo "Update failed ..."
                                               read key
                                             fi
                                             """,
                        file, file, AppRestart.getTerminalRestartCommand()));
                OperationMode.executeAfterShutdown(() -> {
                    TerminalLaunch.builder()
                            .title("XPipe Updater")
                            .localScript(command)
                            .launch();
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
                var command = new ShellScript(String.format(
                        """
                                           runinstaller() {
                                               echo "Installing downloaded .pkg installer ..."
                                               echo "+ sudo installer -verboseR -pkg \\"%s\\" -target /"
                                               sudo installer -verboseR -pkg "%s" -target / || return 1
                                               %s || return 1
                                           }

                                           cd ~
                                           runinstaller
                                           if [ "$?" != 0 ]; then
                                             echo "Update failed ..."
                                             read -rs -k 1 key
                                           fi
                                           """,
                        file, file, AppRestart.getTerminalRestartCommand()));
                OperationMode.executeAfterShutdown(() -> {
                    TerminalLaunch.builder()
                            .title("XPipe Updater")
                            .localScript(command)
                            .launch();
                });
            }

            @Override
            public String getExtension() {
                return "pkg";
            }
        }
    }
}
