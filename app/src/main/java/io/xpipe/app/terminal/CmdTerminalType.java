package io.xpipe.app.terminal;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellDialects;

public class CmdTerminalType extends ExternalTerminalType.SimplePathType implements TrackableTerminalType {

    public CmdTerminalType() {
        super("app.cmd", "cmd.exe", true);
    }

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW;
    }

    @Override
    public int getProcessHierarchyOffset() {
        var powershell = ShellDialects.isPowershell(ProcessControlProvider.get().getEffectiveLocalDialect())
                || AppPrefs.get().enableTerminalLogging().get();
        return powershell ? 0 : -1;
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
    protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
        if (configuration.getScriptDialect().equals(ShellDialects.CMD)) {
            return CommandBuilder.of().add("/c").addFile(configuration.getScriptFile());
        }

        return CommandBuilder.of().add("/c").add(configuration.getDialectLaunchCommand());
    }
}
