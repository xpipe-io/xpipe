package io.xpipe.app.terminal;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.LocalShell;

public class MacOsTerminalType implements ExternalApplicationType.MacApplication, TrackableTerminalType {

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.TABBED;
    }

    @Override
    public boolean isRecommended() {
        return false;
    }

    @Override
    public boolean useColoredTitle() {
        return true;
    }

    @Override
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        LocalShell.getShell()
                .executeSimpleCommand(CommandBuilder.of()
                        .add("open", "-a")
                        .addQuoted("Terminal.app")
                        .addFile(configuration.getScriptFile()));
    }

    @Override
    public int getProcessHierarchyOffset() {
        return 2;
    }

    @Override
    public String getApplicationName() {
        return "Terminal";
    }

    @Override
    public String getId() {
        return "app.macosTerminal";
    }
}
