package io.xpipe.app.terminal;

import io.xpipe.core.process.CommandBuilder;

public class PtyxisTerminalType extends ExternalTerminalType.SimplePathType implements TrackableTerminalType {

    public PtyxisTerminalType() {
        super("app.ptyxis", "ptyxis", true);
    }

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
    protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
        var toExecute = CommandBuilder.of()
                .addIf(configuration.isPreferTabs(), "--tab")
                .addIf(!configuration.isPreferTabs(), "--new-window")
                .add("--")
                .add(configuration.getDialectLaunchCommand());
        return toExecute;
    }
}
