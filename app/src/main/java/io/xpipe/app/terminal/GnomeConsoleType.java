package io.xpipe.app.terminal;

import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;

public class GnomeConsoleType extends ExternalTerminalType.SimplePathType implements TrackableTerminalType {

    public GnomeConsoleType() {
        super("app.gnomeConsole", "kgx", true);
    }

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
    }

    @Override
    public String getWebsite() {
        return "https://apps.gnome.org/en-GB/Console/";
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
                .add(executable)
                .addIf(configuration.isPreferTabs(), "--tab")
                .add("--")
                .add(configuration.getDialectLaunchCommand());
        return toExecute;
    }
}
