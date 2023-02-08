package io.xpipe.ext.proc.augment;

import java.util.List;

public class PowershellCommandAugmentation extends CommandAugmentation {
    @Override
    public boolean matches(String executable) {
        return executable.equals("powershell") || executable.equals("pwsh");
    }

    @Override
    protected void prepareBaseCommand(List<String> baseCommand) {
        remove(baseCommand, "-Command");
        remove(baseCommand, "-NonInteractive");
    }

    @Override
    protected void modifyTerminalCommand(List<String> baseCommand, boolean hasSubCommand) {
        if (hasSubCommand) {
            baseCommand.add("-Command");
        }
    }

    @Override
    protected void modifyNonTerminalCommand(List<String> baseCommand) {
        addIfNeeded(baseCommand, "-NonInteractive");
    }
}
