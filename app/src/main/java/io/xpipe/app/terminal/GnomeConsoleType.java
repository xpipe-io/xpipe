package io.xpipe.app.terminal;

import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;

public class GnomeConsoleType extends ExternalTerminalType.PathCheckType implements TrackableTerminalType {

    public GnomeConsoleType() {
        super("app.gnomeConsole", "kgx", false);
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
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        try (ShellControl pc = LocalShell.getShell()) {
            CommandSupport.isInPathOrThrow(pc, executable, toTranslatedString().getValue(), null);

            var toExecute = CommandBuilder.of()
                    .add(executable)
                    .addIf(configuration.isPreferTabs(), "--tab")
                    .add("--")
                    .add(configuration.getDialectLaunchCommand());
            pc.executeSimpleCommand(toExecute);
        }
    }
}
