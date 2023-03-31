package io.xpipe.app.util;

import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.XPipeInstallation;

import java.nio.file.Files;
import java.nio.file.Path;

public class DesktopShortcuts {

    private static void createWindowsShortcut(String target, String name) throws Exception {
        var icon = XPipeInstallation.getLocalDefaultInstallationIcon();
        var shortcutTarget = XPipeInstallation.getCurrentInstallationBasePath().resolve(XPipeInstallation.getRelativeCliExecutablePath(OsType.WINDOWS));
        var content = String.format(
                """
                        set "TARGET=%s"
                        set "SHORTCUT=%%HOMEDRIVE%%%%HOMEPATH%%\\Desktop\\%s.lnk"
                        set PWS=powershell.exe -ExecutionPolicy Bypass -NoLogo -NonInteractive -NoProfile

                        %%PWS%% -Command "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('%%SHORTCUT%%'); $S.IconLocation='%s'; $S.WindowStyle=7; $S.TargetPath = '%%TARGET%%'; $S.Arguments = 'open %s'; $S.Save()"
                        """,
                shortcutTarget, name, icon.toString(), target);
        LocalStore.getShell().executeSimpleCommand(content);
    }

    private static void createLinuxShortcut(String target, String name) throws Exception {
        var exec = XPipeInstallation.getCurrentInstallationBasePath().resolve(XPipeInstallation.getRelativeCliExecutablePath(OsType.LINUX));
        var icon = XPipeInstallation.getLocalDefaultInstallationIcon();
        var content = String.format(
                """
                        [Desktop Entry]
                        Type=Application
                        Name=%s
                        Comment=Open with X-Pipe
                        Exec="%s" open %s
                        Icon=%s
                        Terminal=false
                        Categories=Utility;Development;Office;
                        """,
                name, exec, target, icon.toString());
        var file = Path.of(System.getProperty("user.home") + "/Desktop/" + name + ".desktop");
        Files.writeString(file, content);
        file.toFile().setExecutable(true);
    }

    private static void createMacOSShortcut(String target, String name) throws Exception {
        var exec = XPipeInstallation.getCurrentInstallationBasePath().resolve(XPipeInstallation.getRelativeCliExecutablePath(OsType.MACOS));
        var icon = XPipeInstallation.getLocalDefaultInstallationIcon();
        var base = System.getProperty("user.home") + "/Desktop/" + name + ".app";
        var content = String.format(
                """
                        #!/bin/bash
                        "%s" open %s
                        """,
                exec, target);

        try (var pc = LocalStore.getShell()) {
            pc.executeSimpleCommand(pc.getShellDialect().getMkdirsCommand(base + "/Contents/MacOS"));
            pc.executeSimpleCommand(pc.getShellDialect().getMkdirsCommand(base + "/Contents/Resources"));

            var executable = base + "/Contents/MacOS/" + name;
            pc.getShellDialect().createTextFileWriteCommand(pc, content, executable).execute();
            pc.executeSimpleCommand("chmod ugo+x \"" + executable + "\"");

            pc.getShellDialect().createTextFileWriteCommand(pc, "APPL????", base + "/PkgInfo").execute();
            pc.executeSimpleCommand("cp \"" + icon + "\" \"" + base + "/Contents/Resources/" + name + ".icns\"");
        }
    }

    public static void create(String target, String name) throws Exception {
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            createWindowsShortcut(target, name);
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            createLinuxShortcut(target, name);
        } else {
            createMacOSShortcut(target, name);
        }
    }
}
