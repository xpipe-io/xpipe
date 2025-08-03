package io.xpipe.app.util;

import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;
import io.xpipe.core.XPipeInstallation;

import java.nio.file.Files;
import java.nio.file.Path;

public class DesktopShortcuts {

    private static Path createWindowsShortcut(String executable, String args, String name) throws Exception {
        var shortcutPath = DesktopHelper.getDesktopDirectory().resolve(name + ".lnk");

        var shell = LocalShell.getLocalPowershell();
        if (shell.isEmpty()) {
            Files.createFile(shortcutPath);
            return shortcutPath;
        }

        var icon = XPipeInstallation.getLocalDefaultInstallationIcon();
        var content = String.format(
                        """
                        $TARGET="%s"
                        $SHORTCUT="%s"
                        $ws = New-Object -ComObject WScript.Shell
                        $s = $ws.CreateShortcut("$SHORTCUT")
                        $S.IconLocation='%s'
                        $S.WindowStyle=7
                        $S.TargetPath = "$TARGET"
                        $S.Arguments = '%s'
                        $S.Save()
                        """,
                        executable, shortcutPath, icon, args)
                .replaceAll("\n", ";");
        shell.get().command(content).execute();
        return shortcutPath;
    }

    private static Path createLinuxShortcut(String executable, String args, String name) throws Exception {
        // Linux .desktop names are very restrictive
        var fixedName = name.replaceAll("[^\\w _]", "");
        var icon = XPipeInstallation.getLocalDefaultInstallationIcon();
        var content = String.format(
                """
                        [Desktop Entry]
                        Type=Application
                        Name=%s
                        Comment=Open with XPipe
                        Exec="%s" %s
                        Icon=%s
                        Terminal=false
                        Categories=Utility;Development;
                        """,
                fixedName, executable, args, icon);
        var file = DesktopHelper.getDesktopDirectory().resolve(name + ".desktop");
        Files.writeString(file, content);
        file.toFile().setExecutable(true);

        // Mark shortcuts as trusted on gnome
        LocalShell.getShell()
                .command(CommandBuilder.of()
                        .add("gio", "set")
                        .addFile(file)
                        .addQuoted("metadata::trusted")
                        .add("true"))
                .executeAndCheck();

        return file;
    }

    private static Path createMacOSShortcut(String executable, String args, String name) throws Exception {
        var icon = XPipeInstallation.getLocalDefaultInstallationIcon();
        var assets = icon.getParent().resolve("Assets.car");
        var base = DesktopHelper.getDesktopDirectory().resolve(name + ".app");
        var content = String.format(
                """
                        #!/usr/bin/env sh
                        "%s" open %s
                        """,
                executable, args);

        try (var pc = LocalShell.getShell()) {
            pc.getShellDialect().deleteFileOrDirectory(pc, base.toString()).executeAndCheck();
            pc.executeSimpleCommand(pc.getShellDialect().getMkdirsCommand(base + "/Contents/MacOS"));
            pc.executeSimpleCommand(pc.getShellDialect().getMkdirsCommand(base + "/Contents/Resources"));

            var macExec = base + "/Contents/MacOS/" + name;
            pc.view().writeScriptFile(FilePath.of(macExec), content);
            pc.executeSimpleCommand("chmod ugo+x \"" + macExec + "\"");

            pc.view().writeTextFile(FilePath.of(base + "/Contents/PkgInfo"), "APPL????");
            pc.view()
                    .writeTextFile(
                            FilePath.of(base + "/Contents/Info.plist"),
                            """
                                                    <?xml version="1.0" encoding="UTF-8"?>
                                                    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                                                    <plist version="1.0">
                                                    <dict>
                                                        <key>CFBundleIconName</key>
                                                        <string>xpipe</string>
                                                    	<key>CFBundleIconFile</key>
                                                    	<string>xpipe</string>
                                                    </dict>
                                                    </plist>
                                                    """);
            pc.command("cp \"" + icon + "\" \"" + base + "/Contents/Resources/xpipe.icns\"")
                    .execute();
            pc.command("cp \"" + assets + "\" \"" + base + "/Contents/Resources/Assets.car\"")
                    .execute();
        }
        return base;
    }

    public static Path createCliOpen(String action, String name) throws Exception {
        var exec = XPipeInstallation.getLocalDefaultCliExecutable();
        return create(exec, "open " + action, name);
    }

    public static Path create(String executable, String args, String name) throws Exception {
        var compat = OsFileSystem.ofLocal().makeFileSystemCompatible(name);
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            return createWindowsShortcut(executable, args, compat);
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            return createLinuxShortcut(executable, args, compat);
        } else {
            return createMacOSShortcut(executable, args, compat);
        }
    }
}
