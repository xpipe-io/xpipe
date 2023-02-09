package io.xpipe.ext.proc.augment;

import io.xpipe.core.process.ShellProcessControl;

import java.util.List;

public class NoCommandAugmentation extends CommandAugmentation {

    @Override
    public boolean matches(String executable) {
        return true;
    }

    @Override
    protected void prepareBaseCommand(ShellProcessControl processControl, List<String> baseCommand) {}

    @Override
    protected void modifyTerminalCommand(List<String> baseCommand, boolean hasSubCommand) {}

    @Override
    protected void modifyNonTerminalCommand(List<String> baseCommand) {}
}
