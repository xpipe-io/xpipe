package io.xpipe.app.terminal;

import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellDialects;

public class CmdTerminalType extends ExternalTerminalType.SimplePathType implements DockableTerminalType {

    public CmdTerminalType() {
        super("app.cmd", "cmd.exe", true);
    }

    @Override
    public int getProcessHierarchyOffset() {
        return -1;
    }

    @Override
    public boolean supportsTabs() {
        return false;
    }

    @Override
    public boolean isRecommended() {
        return false;
    }

    @Override
    public boolean supportsColoredTitle() {
        return false;
    }

    @Override
    protected CommandBuilder toCommand(LaunchConfiguration configuration) {
        if (configuration.getScriptDialect().equals(ShellDialects.CMD)) {
            return CommandBuilder.of().add("/c").addFile(configuration.getScriptFile());
        }

        return CommandBuilder.of().add("/c").add(configuration.getDialectLaunchCommand());
    }
}
