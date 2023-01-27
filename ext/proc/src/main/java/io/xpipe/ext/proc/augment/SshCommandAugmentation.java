package io.xpipe.ext.proc.augment;

import java.util.List;

public class SshCommandAugmentation extends CommandAugmentation {

    @Override
    public boolean matches(String executable) {
        return executable.equals("ssh");
    }

    @Override
    protected void prepareBaseCommand(List<String> baseCommand) {
        baseCommand.removeIf(s -> s.equals("-T"));
        baseCommand.removeIf(s -> s.equals("-t"));
        baseCommand.removeIf(s -> s.equals("-tt"));
        baseCommand.removeIf(s -> s.equals("-oStrictHostKeyChecking=yes"));

        addIfNeeded(baseCommand, "-oStrictHostKeyChecking=no");
        // addIfNeeded(baseCommand,"-oPasswordAuthentication=no");
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
