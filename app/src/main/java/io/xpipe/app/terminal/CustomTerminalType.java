package io.xpipe.app.terminal;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
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
        try (var sc = LocalShell.getShell()) {
            var toExecute = ExternalApplicationHelper.replaceVariableArgument(
                    format, "CMD", configuration.single().getScriptFile().toString());
            // We can't be sure whether the command is blocking or not, so always make it not blocking
            if (sc.getOsType() == OsType.WINDOWS) {
                toExecute = "start \"" + configuration.getCleanTitle() + "\" " + toExecute;
            } else {
                var async = sc.getShellDialect().launchAsync(CommandBuilder.of().add(toExecute), true);
                toExecute = async.buildFull(sc);
            }
            sc.command(toExecute).execute();
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
