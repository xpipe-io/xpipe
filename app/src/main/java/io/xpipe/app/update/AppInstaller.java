package io.xpipe.app.update;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.app.util.TerminalHelper;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.CommandControl;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.XPipeInstallation;
import lombok.Getter;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class AppInstaller {

    public static void installOnRemoteMachine(ShellControl s, String version) throws Exception {
        var asset = getSuitablePlatformAsset(s);
        var file = AppDownloads.downloadInstaller(asset, version, false);
        if (file.isEmpty()) {
            return;
        }

        installFile(s, asset, file.get());
    }

    public static void installFileLocal(InstallerAssetType asset, Path localFile) throws Exception {
        asset.installLocal(localFile.toString());
    }

    public static void installFile(ShellControl s, InstallerAssetType asset, Path localFile) throws Exception {
        String targetFile = null;
        if (s.isLocal()) {
            targetFile = localFile.toString();
        } else {
            targetFile = FileNames.join(
                    s.getTemporaryDirectory(), localFile.getFileName().toString());
            try (InputStream in = Files.newInputStream(localFile)) {
                in.transferTo(s.getShellDialect().createStreamFileWriteCommand(s, targetFile).startExternalStdin());
            }
        }

        asset.installRemote(s, targetFile);
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

    public static InstallerAssetType getSuitablePlatformAsset(ShellControl p) throws Exception {
        if (p.getOsType().equals(OsType.WINDOWS)) {
            return new InstallerAssetType.Msi();
        }

        if (p.getOsType().equals(OsType.LINUX)) {
            try (CommandControl c = p.getShellDialect().createFileExistsCommand(p, "/etc/debian_version")
                    .start()) {
                return c.discardAndCheckExit() ? new InstallerAssetType.Debian() : new InstallerAssetType.Rpm();
            }
        }

        if (p.getOsType().equals(OsType.MACOS)) {
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

        public abstract void installRemote(ShellControl pc, String file) throws Exception;

        public abstract void installLocal(String file) throws Exception;

        public boolean isCorrectAsset(String name) {
            return name.endsWith(getExtension());
        }

        public abstract String getExtension();

        @JsonTypeName("msi")
        public static final class Msi extends InstallerAssetType {

            @Override
            public String getExtension() {
                return ".msi";
            }

            @Override
            public void installRemote(ShellControl shellControl, String file) throws Exception {
                var exec = XPipeInstallation.getInstallationExecutable(
                        shellControl,
                        XPipeInstallation.getDefaultInstallationBasePath(shellControl, false));
                var logsDir = FileNames.join(XPipeInstallation.getDataBasePath(shellControl), "logs");
                var cmd = new ArrayList<>(java.util.List.of(
                        "start",
                        "/wait",
                        "msiexec",
                        "/i",
                        file,
                        "/l*",
                        FileNames.join(logsDir, "installer_" + FileNames.getFileName(file) + ".log"),
                        "/qb",
                        "&",
                        exec
                        // "/qf"
                        ));
                try (CommandControl c = shellControl.command(cmd).start()) {
                    c.discardOrThrow();
                }
            }

            @Override
            public void installLocal(String file) throws Exception {
                var shellProcessControl = ShellStore.createLocal().create().start();
                var exec = XPipeInstallation.getInstallationExecutable(
                        shellProcessControl,
                        XPipeInstallation.getDefaultInstallationBasePath(shellProcessControl, false));
                var logsDir = FileNames.join(XPipeInstallation.getDataBasePath(shellProcessControl), "logs");
                var logFile = FileNames.join(logsDir, "installer_" + FileNames.getFileName(file) + ".log");
                var script = ScriptHelper.createExecScript(shellProcessControl, String.format(
                        """
                                cd /D "%%HOMEDRIVE%%%%HOMEPATH%%"
                                start "" /wait msiexec /i "%s" /l* "%s" /qb
                                start "" "%s"
                                """,
                        file, logFile, exec));
                shellProcessControl.executeSimpleCommand("start \"\" /min \"" + script + "\"");
            }
        }

        @JsonTypeName("debian")
        public static final class Debian extends InstallerAssetType {

            @Override
            public String getExtension() {
                return ".deb";
            }

            @Override
            public void installRemote(ShellControl shellControl, String file) throws Exception {
                try (var pc = shellControl.subShell(ShellDialects.BASH).start()) {
                    try (CommandControl c = pc.command("DEBIAN_FRONTEND=noninteractive apt-get remove -qy xpipe")
                            .elevated()
                            .start()) {
                        c.discardOrThrow();
                    }
                    try (CommandControl c = pc.command(
                                    "DEBIAN_FRONTEND=noninteractive apt-get install -qy \"" + file + "\"")
                            .elevated()
                            .start()) {
                        c.discardOrThrow();
                    }
                    pc.executeSimpleCommand("xpipe open");
                }
            }

            @Override
            public void installLocal(String file) throws Exception {
                var command = ShellStore.createLocal()
                        .create()
                        .subShell(ShellDialects.BASH)
                        .command(String.format(
                                """
                                        function exec {
                                            echo "+ sudo apt-get remove -qy xpipe"
                                            echo "+ sudo apt-get install -qy \\"%s\\""
                                            DEBIAN_FRONTEND=noninteractive sudo apt-get remove -qy xpipe || return 1
                                            DEBIAN_FRONTEND=noninteractive sudo apt-get install -qy "%s" || return 1
                                            xpipe open || return 1
                                        }

                                        cd ~
                                        exec || read -rsp "Update failed ..."$'\\n' -n 1 key
                                        """,
                                file, file));
                TerminalHelper.open("X-Pipe Updater", command);
            }
        }

        @JsonTypeName("rpm")
        public static final class Rpm extends InstallerAssetType {
            @Override
            public String getExtension() {
                return ".rpm";
            }

            @Override
            public void installRemote(ShellControl shellControl, String file) throws Exception {
                try (var pc = shellControl.subShell(ShellDialects.BASH).start()) {
                    try (CommandControl c = pc.command("rpm -U -v --force \"" + file + "\"")
                            .elevated()
                            .start()) {
                        c.discardOrThrow();
                    }
                    pc.executeSimpleCommand("xpipe open");
                }
            }

            @Override
            public void installLocal(String file) throws Exception {
                var command = ShellStore.createLocal().create().subShell(ShellDialects.BASH).command(String.format(
                        """
                                        function exec {
                                            echo "+ sudo rpm -U -v --force \\"%s\\""
                                            sudo rpm -U -v --force "%s" || return 1
                                            xpipe open || return 1
                                        }

                                        cd ~
                                        exec || read -rsp "Update failed ..."$'\\n' -n 1 key
                                        """,
                        file, file));
                TerminalHelper.open("X-Pipe Updater", command);
            }
        }

        @JsonTypeName("pkg")
        public static final class Pkg extends InstallerAssetType {
            @Override
            public String getExtension() {
                return ".pkg";
            }

            @Override
            public void installRemote(ShellControl shellControl, String file) throws Exception {
                try (var pc = shellControl.subShell(ShellDialects.BASH).start()) {
                    try (CommandControl c = pc.command(
                                    "installer -verboseR -allowUntrusted -pkg \"" + file + "\" -target /")
                            .elevated()
                            .start()) {
                        c.discardOrThrow();
                    }
                    pc.executeSimpleCommand("xpipe open");
                }
            }

            @Override
            public void installLocal(String file) throws Exception {
                var command = ShellStore.createLocal().create().subShell(ShellDialects.BASH).command(String.format(
                        """
                                        function exec {
                                            echo "+ sudo installer -verboseR -allowUntrusted -pkg \\"%s\\" -target /"
                                            sudo installer -verboseR -allowUntrusted -pkg "%s" -target / || return 1
                                            xpipe open || return 1
                                        }

                                        cd ~
                                        exec || read -rsp "Update failed ..."$'\\n' -n 1 key
                                        """,
                        file, file));
                TerminalHelper.open("X-Pipe Updater", command);
            }
        }
    }
}
