package io.xpipe.app.terminal;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.util.LocalShell;

public class CmdTerminalType
        implements ExternalApplicationType.PathApplication, ExternalTerminalType, TrackableTerminalType {

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW;
    }

    @Override
    public boolean isRecommended() {
        return false;
    }

    @Override
    public boolean useColoredTitle() {
        return false;
    }

    @Override
    public boolean supportsEscapes() {
        return false;
    }

    @Override
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        var args = toCommand(configuration);
        launch(args);
    }

    @Override
    public int getProcessHierarchyOffset() {
        var powershell = ShellDialects.isPowershell(LocalShell.getDialect());
        return powershell ? 0 : -1;
    }

    private CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
        if (configuration.getScriptDialect() == ShellDialects.CMD) {
            return CommandBuilder.of().add("/c").addFile(configuration.getScriptFile());
        }

        return CommandBuilder.of().add("/c").add(configuration.getDialectLaunchCommand());
    }

    @Override
    public String getExecutable() {
        return "cmd.exe";
    }

    @Override
    public boolean detach() {
        return true;
    }

    @Override
    public String getId() {
        return "app.cmd";
    }
}
