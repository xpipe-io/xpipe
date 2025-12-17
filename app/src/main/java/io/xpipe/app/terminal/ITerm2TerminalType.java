package io.xpipe.app.terminal;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;

public class ITerm2TerminalType implements ExternalApplicationType.MacApplication, TrackableTerminalType {

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.TABBED;
    }

    @Override
    public String getWebsite() {
        return "https://iterm2.com/";
    }

    @Override
    public boolean isRecommended() {
        return true;
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
                        .addQuoted("iTerm.app")
                        .addFile(configuration.single().getScriptFile()));
    }

    @Override
    public int getProcessHierarchyOffset() {
        return 3;
    }

    @Override
    public String getApplicationName() {
        return "iTerm";
    }

    @Override
    public String getId() {
        return "app.iterm2";
    }
}
