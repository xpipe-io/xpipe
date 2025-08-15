package io.xpipe.app.terminal;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.OsType;

import java.util.Locale;

public class CustomTerminalType implements ExternalApplicationType, ExternalTerminalType {

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW;
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
        var custom = AppPrefs.get().customTerminalCommand().getValue();
        if (custom == null || custom.isBlank()) {
            throw ErrorEventFactory.expected(new IllegalStateException("No custom terminal command specified"));
        }

        var format = custom.toLowerCase(Locale.ROOT).contains("$cmd") ? custom : custom + " $CMD";
        try (var pc = LocalShell.getShell()) {
            var toExecute = ExternalApplicationHelper.replaceVariableArgument(
                    format, "CMD", configuration.getScriptFile().toString());
            // We can't be sure whether the command is blocking or not, so always make it not blocking
            if (pc.getOsType() == OsType.WINDOWS) {
                toExecute = "start \"" + configuration.getCleanTitle() + "\" " + toExecute;
            } else {
                toExecute = "nohup " + toExecute + " </dev/null &>/dev/null & disown";
            }
            pc.executeSimpleCommand(toExecute);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getId() {
        return "app.custom";
    }
}
