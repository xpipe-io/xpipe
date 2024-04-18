package io.xpipe.app.update;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.app.util.TerminalLauncher;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.LocalStore;
import io.xpipe.core.util.XPipeInstallation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;

import java.nio.file.Files;
import java.nio.file.Path;

public class AppInstaller {

    public static void installFileLocal(InstallerAssetType asset, Path localFile) throws Exception {
        asset.installLocal(localFile.toString());
    }

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

        public abstract void installLocal(String file) throws Exception;

        public boolean isCorrectAsset(String name) {
            return name.endsWith(getExtension())
                    && name.contains(AppProperties.get().getArch());
        }

        public abstract String getExtension();

        @JsonTypeName("msi")
        public static final class Msi extends InstallerAssetType {

            @Override
            public void installLocal(String file) throws Exception {
                var shellProcessControl = new LocalStore().control().start();
                var exec = (AppProperties.get().isDevelopmentEnvironment()
                                ? Path.of(XPipeInstallation.getLocalDefaultInstallationBasePath())
                                : XPipeInstallation.getCurrentInstallationBasePath())
                        .resolve(XPipeInstallation.getDaemonExecutablePath(OsType.getLocal()))
                        .toString();
                var logsDir = FileNames.join(XPipeInstallation.getDataDir().toString(), "logs");
                var logFile = FileNames.join(logsDir, "installer_" + FileNames.getFileName(file) + ".log");
                var command = LocalShell.getShell().getShellDialect().equals(ShellDialects.CMD)
                        ? getCmdCommand(file, logFile, exec)
                        : getPowershellCommand(file, logFile, exec);
                var toRun = LocalShell.getShell().getShellDialect().equals(ShellDialects.CMD)
                        ? "start \"XPipe Updater\" /min cmd /c \"" + ScriptHelper.createLocalExecScript(command) + "\""
                        : "Start-Process -WindowStyle Minimized -FilePath powershell -ArgumentList  \"-ExecutionPolicy\", \"Bypass\", \"-File\", \"`\""
                                + ScriptHelper.createLocalExecScript(command) + "`\"\"";
                shellProcessControl.executeSimpleCommand(toRun);
            }

            @Override
            public String getExtension() {
                return ".msi";
            }

            private String getCmdCommand(String file, String logFile, String exec) {
                return String.format(
                        """
                        echo Installing %s ...
                        cd /D "%%HOMEDRIVE%%%%HOMEPATH%%"
                        echo + msiexec /i "%s" /lv "%s" /qr
                        start "" /wait msiexec /i "%s" /lv "%s" /qb
                        echo Starting XPipe ...
                        echo + "%s"
                        start "" "%s"
                        """,
                        file, file, logFile, file, logFile, exec, exec);
            }

            private String getPowershellCommand(String file, String logFile, String exec) {
                return String.format(
                        """
                        echo Installing %s ...
                        cd "$env:HOMEDRIVE\\$env:HOMEPATH"
                        echo '+ msiexec /i "%s" /lv "%s" /qr'
                        Start-Process msiexec -Wait -ArgumentList "/i", "`"%s`"", "/lv", "`"%s`"", "/qb"
                        echo 'Starting XPipe ...'
                        echo '+ "%s"'
                        Start-Process -FilePath "%s"
                        """,
                        file, file, logFile, file, logFile, exec, exec);
            }
        }

        @JsonTypeName("debian")
        public static final class Debian extends InstallerAssetType {

            @Override
            public void installLocal(String file) throws Exception {
                var name = AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe";
                var command = String.format(
                        """
                                             function exec {
                                                 echo "+ sudo apt install \\"%s\\""
                                                 DEBIAN_FRONTEND=noninteractive sudo apt-get install -qy "%s" || return 1
                                                 %s open || return 1
                                             }

                                             cd ~
                                             exec || read -rsp "Update failed ..."$'\\n' -n 1 key
                                             """,
                        file, file, name);
                TerminalLauncher.openDirect("XPipe Updater", sc -> command);
            }

            @Override
            public String getExtension() {
                return ".deb";
            }
        }

        @JsonTypeName("rpm")
        public static final class Rpm extends InstallerAssetType {
            @Override
            public void installLocal(String file) throws Exception {
                var name = AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe";
                var command = String.format(
                        """
                                             function exec {
                                                 echo "+ sudo rpm -U -v --force \\"%s\\""
                                                 sudo rpm -U -v --force "%s" || return 1
                                                 %s open || return 1
                                             }

                                             cd ~
                                             exec || read -rsp "Update failed ..."$'\\n' -n 1 key
                                             """,
                        file, file, name);
                TerminalLauncher.openDirect("XPipe Updater", sc -> command);
            }

            @Override
            public String getExtension() {
                return ".rpm";
            }
        }

        @JsonTypeName("pkg")
        public static final class Pkg extends InstallerAssetType {
            @Override
            public void installLocal(String file) throws Exception {
                var name = AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe";
                var command = String.format(
                        """
                                           function exec {
                                               echo "+ sudo installer -verboseR -allowUntrusted -pkg \\"%s\\" -target /"
                                               sudo installer -verboseR -allowUntrusted -pkg "%s" -target / || return 1
                                               %s open || return 1
                                           }

                                           cd ~
                                           exec || echo "Update failed ..." && read -rs -k 1 key
                                           """,
                        file, file, name);
                TerminalLauncher.openDirect("XPipe Updater", sc -> command);
            }

            @Override
            public String getExtension() {
                return ".pkg";
            }
        }
    }
}
