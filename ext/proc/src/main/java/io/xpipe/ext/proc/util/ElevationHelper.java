package io.xpipe.ext.proc.util;

import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellTypes;
import io.xpipe.extension.util.ScriptHelper;
import lombok.SneakyThrows;

public class ElevationHelper {

    @SneakyThrows
    public static String elevateNormalCommand(String command, ShellProcessControl parent, String displayName) {
        if (parent.getOsType().equals(OsType.WINDOWS)) {
            return command;
        }

        if (parent.getElevationPassword() == null) {
            var c = "SUDO_ASKPASS=/bin/false sudo -A -p \"\" -- "
                    + parent.getShellType().executeCommandWithShell(command);
            return c;
        }

        var file = ScriptHelper.createAskPassScript(parent.getElevationPassword(), parent, parent.getShellType(), true);
        return "SUDO_ASKPASS=\"" + file + "\" sudo -k -p \"\" -A -- "
                + parent.getShellType().executeCommandWithShell(command);
    }

    @SneakyThrows
    public static String elevateTerminalCommand(String command, ShellProcessControl parent) {
        if (parent.getOsType().equals(OsType.WINDOWS)) {
            return command;
        }

        if (parent.getElevationPassword() == null) {
            return "SUDO_ASKPASS=/bin/false sudo -n -p \"\" -A -- " + command;
        }

        var scriptType = parent.getShellType();

        // Fix for power shell as there are permission issues when executing a powershell askpass script
        if (parent.getShellType().equals(ShellTypes.POWERSHELL)) {
            scriptType = parent.getOsType().equals(OsType.WINDOWS) ? ShellTypes.CMD : ShellTypes.BASH;
        }

        var file = ScriptHelper.createAskPassScript(parent.getElevationPassword(), parent, scriptType, true);
        var cmd = "SUDO_ASKPASS=\"" + file + "\" sudo -k -p \"\" -A -- " + command;

        return cmd;
    }
}
