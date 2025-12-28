package io.xpipe.app.terminal;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.FlatpakCache;

public class PtyxisTerminalType implements ExternalApplicationType.LinuxApplication, TrackableTerminalType {

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
                .add(configuration.single().getDialectLaunchCommand());
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

    @Override
    public String getFlatpakId() throws Exception {
        var dev = FlatpakCache.getApp("org.gnome.Ptyxis.Devel");
        if (dev.isPresent()) {
            return "org.gnome.Ptyxis.Devel";
        }

        return "app.devsuite.Ptyxis";
    }
}
