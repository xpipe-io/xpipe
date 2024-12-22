package io.xpipe.app.util;

import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.XPipeInstallation;

import java.nio.file.Files;
import java.nio.file.Path;

public class DesktopShortcuts {

    private static Path createWindowsShortcut(String executable, String args, String name) throws Exception {
        var icon = XPipeInstallation.getLocalDefaultInstallationIcon();
        var shortcutPath = DesktopHelper.getDesktopDirectory().resolve(name + ".lnk");
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
        LocalShell.getLocalPowershell().executeSimpleCommand(content);
        return shortcutPath;
    }

    private static Path createLinuxShortcut(String executable, String args, String name) throws Exception {
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
                name, executable, args, icon);
        var file = DesktopHelper.getDesktopDirectory().resolve(name + ".desktop");
        Files.writeString(file, content);
        file.toFile().setExecutable(true);
        return file;
    }

    private static Path createMacOSShortcut(String executable, String args, String name) throws Exception {
        var icon = XPipeInstallation.getLocalDefaultInstallationIcon();
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
            pc.view().writeScriptFile(new FilePath(macExec), content);
            pc.executeSimpleCommand("chmod ugo+x \"" + macExec + "\"");

            pc.view().writeTextFile(new FilePath(base + "/Contents/PkgInfo"), "APPL????");
            pc.view().writeTextFile(new FilePath(base + "/Contents/Info.plist"), """
                                                    <?xml version="1.0" encoding="UTF-8"?>
                                                    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                                                    <plist version="1.0">
                                                    <dict>
                                                    	<key>CFBundleIconFile</key>
                                                    	<string>icon.icns</string>
                                                    </dict>
                                                    </plist>
                                                    """);
            pc.executeSimpleCommand("cp \"" + icon + "\" \"" + base + "/Contents/Resources/icon.icns\"");
        }
        return base;
    }

    public static Path createCliOpen(String action, String name) throws Exception {
        var exec = XPipeInstallation.getLocalDefaultCliExecutable();
        return create(exec, "open " + action, name);
    }

    public static Path create(String executable, String args, String name) throws Exception {
        var compat = OsType.getLocal().makeFileSystemCompatible(name);
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            return createWindowsShortcut(executable, args, compat);
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            return createLinuxShortcut(executable, args, compat);
        } else {
            return createMacOSShortcut(executable, args, compat);
        }
    }
}
