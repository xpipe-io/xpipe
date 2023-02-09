package io.xpipe.ext.proc.augment;

import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;

import java.util.List;

public class SshCommandAugmentation extends CommandAugmentation {

    @Override
    public boolean matches(String executable) {
        return executable.equals("ssh");
    }

    @Override
    protected void prepareBaseCommand(ShellProcessControl processControl, List<String> baseCommand) throws Exception {
        baseCommand.removeIf(s -> s.equals("-T"));
        baseCommand.removeIf(s -> s.equals("-t"));
        baseCommand.removeIf(s -> s.equals("-tt"));
        baseCommand.removeIf(s -> s.equals("-oStrictHostKeyChecking=yes"));

        addIfNeeded(baseCommand, "-oStrictHostKeyChecking=no");
        // addIfNeeded(baseCommand,"-oPasswordAuthentication=no");

        // Ensure proper permissions for keys
        var key = getParameter(baseCommand, "-i");
        if (key.isPresent() && (processControl.getOsType().equals(OsType.LINUX) || processControl.getOsType().equals(OsType.MACOS)) ){
            processControl.executeSimpleCommand("chmod 400 \"" + key.get() + "\"");
        }

        // Start agent on windows
        if (processControl.getOsType().equals(OsType.WINDOWS)) {
            processControl.executeBooleanSimpleCommand("ssh-agent start");
        }
    }

    @Override
    protected void modifyTerminalCommand(List<String> baseCommand, boolean hasSubCommand) {
        addIfNeeded(baseCommand, "-t");
    }

    @Override
    protected void modifyNonTerminalCommand(List<String> baseCommand) {
        addIfNeeded(baseCommand, "-T");
    }
}
