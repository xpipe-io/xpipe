package io.xpipe.app.terminal;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;

public class GnomeTerminalType implements ExternalApplicationType.PathApplication, TrackableTerminalType {

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW;
    }

    @Override
    public String getWebsite() {
        return "https://help.gnome.org/users/gnome-terminal/stable/";
    }

    @Override
    public boolean isRecommended() {
        return AppPrefs.get().terminalMultiplexer().getValue() != null;
    }

    @Override
    public boolean useColoredTitle() {
        return false;
    }

    @Override
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        try (ShellControl pc = LocalShell.getShell()) {
            CommandSupport.isInPathOrThrow(
                    pc, getExecutable(), toTranslatedString().getValue(), null);

            var toExecute = CommandBuilder.of()
                    .add(getExecutable(), "-v", "--title")
                    .addQuoted(configuration.getColoredTitle())
                    .add("--")
                    .addFile(configuration.getScriptFile())
                    // In order to fix this bug which also affects us:
                    // https://askubuntu.com/questions/1148475/launching-gnome-terminal-from-vscode
                    .environment("GNOME_TERMINAL_SCREEN", sc -> "");
            pc.executeSimpleCommand(toExecute);
        }
    }

    @Override
    public String getExecutable() {
        return "gnome-terminal";
    }

    @Override
    public boolean detach() {
        return true;
    }

    @Override
    public String getId() {
        return "app.gnomeTerminal";
    }
}
