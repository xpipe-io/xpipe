package io.xpipe.app.terminal;

import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;

public class PtyxisTerminalType extends ExternalTerminalType.PathCheckType implements TrackableTerminalType {

    public PtyxisTerminalType() {
        super("app.ptyxis", "ptyxis", false);
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
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        try (ShellControl pc = LocalShell.getShell()) {
            CommandSupport.isInPathOrThrow(pc, executable, toTranslatedString().getValue(), null);

            var toExecute = CommandBuilder.of()
                    .add(executable)
                    .addIf(configuration.isPreferTabs(), "--tab")
                    .addIf(!configuration.isPreferTabs(), "--new-window")
                    .add("--")
                    .add(configuration.getDialectLaunchCommand());
            // This returns a failure exit code even if the terminal opened correctly
            pc.command(toExecute).executeAndCheck();
        }
    }
}
