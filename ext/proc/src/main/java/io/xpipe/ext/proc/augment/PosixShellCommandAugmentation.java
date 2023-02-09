package io.xpipe.ext.proc.augment;

import io.xpipe.core.process.ShellProcessControl;

import java.util.List;

public class PosixShellCommandAugmentation extends CommandAugmentation {
    @Override
    public boolean matches(String executable) {
        return executable.equals("sh") || executable.equals("bash") || executable.equals("zsh");
    }

    @Override
    protected void prepareBaseCommand(ShellProcessControl processControl, List<String> baseCommand) {
        remove(baseCommand, "-l", "--login");
        remove(baseCommand, "-i");
        remove(baseCommand, "-c");
        remove(baseCommand, "-s");
    }

    @Override
    protected void modifyTerminalCommand(List<String> baseCommand, boolean hasSubCommand) {
        addIfNeeded(baseCommand, "-i");
        if (hasSubCommand) {
            baseCommand.add("-c");
        }
    }

    @Override
    protected void modifyNonTerminalCommand(List<String> baseCommand) {}
}
