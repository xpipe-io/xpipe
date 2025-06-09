package io.xpipe.app.terminal;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.core.process.CommandBuilder;

public class PtyxisTerminalType implements ExternalApplicationType.PathApplication, TrackableTerminalType {

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
    }

    @Override
    public String getWebsite() {
        return "https://gitlab.gnome.org/chergert/ptyxis";
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
        var toExecute = CommandBuilder.of()
                .addIf(configuration.isPreferTabs(), "--tab")
                .addIf(!configuration.isPreferTabs(), "--new-window")
                .add("--")
                .add(configuration.getDialectLaunchCommand());
        launch(toExecute);
    }

    @Override
    public String getExecutable() {
        return "ptyxis";
    }

    @Override
    public boolean detach() {
        return true;
    }

    @Override
    public String getId() {
        return "app.ptyxis";
    }
}
