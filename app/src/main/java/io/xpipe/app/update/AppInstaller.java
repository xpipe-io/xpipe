package io.xpipe.app.update;

import io.xpipe.app.core.*;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ScriptHelper;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.FilePath;
import io.xpipe.app.util.OsType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

public class AppInstaller {

    public static InstallerAssetType getSuitablePlatformAsset() {
        if (OsType.ofLocal() == OsType.WINDOWS) {
            return new InstallerAssetType.Msi();
        }

        if (OsType.ofLocal() == OsType.LINUX) {
            return AppSystemInfo.ofLinux().isDebianBased()
                    ? new InstallerAssetType.Debian()
                    : new InstallerAssetType.Rpm();
        }

        if (OsType.ofLocal() == OsType.MACOS) {
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
                installLocal(file, false);
            }

            public void installLocal(Path file, boolean uninstall) {
                var logsDir =
                        AppLogs.get().getSessionLogsDirectory().getParent().toString();
                var logFile = FilePath.of(logsDir, "installer.log");
                var systemWide = isSystemWide();
                var cmdScript = LocalShell.getDialect() == ShellDialects.CMD && !systemWide;
                var command = cmdScript
                        ? getCmdCommand(file.toString(), logFile.toString(), uninstall)
                        : getPowershellCommand(file.toString(), logFile.toString(), uninstall, systemWide);

                AppOperationMode.executeAfterShutdown(() -> {
                    try (var sc = LocalShell.getShell().start()) {
                        String toRun;
                        if (cmdScript) {
                            toRun = "start \"" + AppNames.ofCurrent().getName() + " Updater\" /min cmd /c \""
                                    + ScriptHelper.createExecScript(ShellDialects.CMD, sc, command) + "\"";
                        } else {
                            toRun = sc.getShellDialect() == ShellDialects.POWERSHELL
                                    ? "Start-Process -WindowStyle Minimized -FilePath powershell -ArgumentList  \"-ExecutionPolicy\", \"Bypass\", "
                                      + "\"-File\", \"`\""
                                      + ScriptHelper.createExecScript(ShellDialects.POWERSHELL, sc, command)
                                      + "`\"\""
                                    : "start \"" + AppNames.ofCurrent().getName()
                                      + " Updater\" /min powershell -ExecutionPolicy Bypass -File \""
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

            private Optional<String> getProductCode() {
                var sc = LocalShell.getLocalPowershell();
                if (sc.isEmpty()) {
                    return Optional.empty();
                }

                try {
                    var context = isSystemWide() ? "4" : "3";
                    var name = AppProperties.get().isStaging() ? "XPipe PTB" : "XPipe";
                    var out = sc.get().command("""
                                               $Installer = New-Object -ComObject WindowsInstaller.Installer
                                               $InstallerProducts = $Installer.ProductsEx("", "", %s)
                                               $Product = $InstallerProducts | Where-Object { $_.InstallProperty("ProductName") -eq '%s' }
                                               echo $Product.ProductCode()
                                               """.formatted(context, name).lines().collect(Collectors.joining(";"))).readStdoutIfPossible();
                    return out.filter(s -> !s.isEmpty());
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).omit().handle();
                    return Optional.empty();
                }
            }

            private String getCmdCommand(String file, String logFile, boolean uninstall) {
                String uninstallCommand = "";
                if (uninstall) {
                    var id = getProductCode();
                    if (id.isPresent()) {
                        uninstallCommand = """
                                               echo + msiexec /x "%s"
                                               start "" /wait msiexec /x "%s" /qb
                                               """.formatted(id.get(), id.get());
                    }
                }

                var args = "MSIFASTINSTALL=7 DISABLEROLLBACK=1";
                return String.format(
                        """
                                     echo Installing %s ...
                                     cd /D "%%HOMEDRIVE%%%%HOMEPATH%%"
                                     %s
                                     echo + msiexec /i "%s" /lv "%s" /qb %s
                                     start "" /wait msiexec /i "%s" /lv "%s" /qb %s
                                     %s
                                     """,
                        file,
                        uninstallCommand,
                        file,
                        logFile,
                        args,
                        file,
                        logFile,
                        args,
                        AppRestart.getBackgroundRestartCommand(
                                AppProperties.get().getDataDir(), null, ShellDialects.CMD));
            }

            private String getPowershellCommand(String file, String logFile, boolean uninstall, boolean systemWide) {
                var property = "MSIFASTINSTALL=7 DISABLEROLLBACK=1" + (systemWide ? " ALLUSERS=1" : "");
                var startProcessProperty = ", MSIFASTINSTALL=7, DISABLEROLLBACK=1" + (systemWide ? ", ALLUSERS=1" : "");
                var runas = systemWide ? "-Verb runAs" : "";
                String uninstallCommand = "";
                if (uninstall) {
                    var id = getProductCode();
                    if (id.isPresent()) {
                        uninstallCommand = """
                                echo '+ msiexec /x "%s"'
                                Start-Process %s -FilePath msiexec -Wait -ArgumentList "/x", "`"%s`"", "/qb"
                                """.formatted(runas, id.get(), id.get());
                    }
                }
                return String.format(
                        """
                                     echo Installing %s ...
                                     cd "$env:HOMEDRIVE\\$env:HOMEPATH"
                                     %s
                                     echo '+ msiexec /i "%s" /lv "%s" /qb%s'
                                     Start-Process %s -FilePath msiexec -Wait -ArgumentList "/i", "`"%s`"", "/lv", "`"%s`"", "/qb"%s
                                     %s
                                     """,
                        file,
                        uninstall ? uninstallCommand : "",
                        file,
                        logFile,
                        property,
                        runas,
                        file,
                        logFile,
                        startProcessProperty,
                        AppRestart.getBackgroundRestartCommand(
                                AppProperties.get().getDataDir(), null, ShellDialects.POWERSHELL));
            }
        }

        @JsonTypeName("debian")
        public static final class Debian extends InstallerAssetType {

            @Override
            public void installLocal(Path file) {
                var command = new ShellScript(String.format("""
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
                                                            """, file, file, AppRestart.getTerminalRestartCommand()));
                AppOperationMode.executeAfterShutdown(() -> {
                    TerminalLaunch.builder()
                            .title(AppNames.ofCurrent().getName() + " Updater")
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
                var command = new ShellScript(String.format("""
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
                                                            """, file, file, AppRestart.getTerminalRestartCommand()));
                AppOperationMode.executeAfterShutdown(() -> {
                    TerminalLaunch.builder()
                            .title(AppNames.ofCurrent().getName() + " Updater")
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
                var command = new ShellScript(String.format("""
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
                                                              read key
                                                            fi
                                                            """, file, file, AppRestart.getTerminalRestartCommand()));
                AppOperationMode.executeAfterShutdown(() -> {
                    TerminalLaunch.builder()
                            .title(AppNames.ofCurrent().getName() + " Updater")
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
