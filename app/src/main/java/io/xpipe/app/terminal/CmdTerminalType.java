package io.xpipe.app.terminal;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellDialects;

public class CmdTerminalType
        implements ExternalApplicationType.PathApplication, ExternalTerminalType, TrackableTerminalType {

    @Override
    public boolean supportsEscapes() {
        return false;
    }

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW;
    }

    @Override
    public int getProcessHierarchyOffset() {
        var powershell = ShellDialects.isPowershell(ProcessControlProvider.get().getEffectiveLocalDialect());
        return powershell ? 0 : -1;
    }

    @Override
    public boolean isRecommended() {
        return false;
    }

    @Override
    public boolean useColoredTitle() {
        return false;
    }

    private CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
        if (configuration.getScriptDialect().equals(ShellDialects.CMD)) {
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

    @Override
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        var args = toCommand(configuration);
        launch(args);
    }
}
