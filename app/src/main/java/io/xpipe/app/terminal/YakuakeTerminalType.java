package io.xpipe.app.terminal;

import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.util.FlatpakCache;

public class YakuakeTerminalType implements ExternalApplicationType.PathApplication, TrackableTerminalType {

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.TABBED;
    }

    @Override
    public String getWebsite() {
        return "https://apps.kde.org/en-gb/yakuake/";
    }

    @Override
    public boolean isRecommended() {
        return false;
    }

    @Override
    public boolean useColoredTitle() {
        return false;
    }

    @Override
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        CommandSupport.isInLocalPathOrThrow("Yakuake", "yakuake");

        var toggle = CommandBuilder.of().add("qdbus", "org.kde.yakuake", "/yakuake/window", "org.kde.yakuake.toggleWindowState");
        LocalShell.getShell().command(toggle).execute();

        var newTab = CommandBuilder.of().add("qdbus", "org.kde.yakuake", "/yakuake/sessions","org.kde.yakuake.addSession");
        var index = LocalShell.getShell().command(newTab).readStdoutOrThrow();

        var renameTab = CommandBuilder.of().add("qdbus", "org.kde.yakuake", "/yakuake/tabs", "setTabTitle", index).addLiteral(configuration.getColoredTitle());
        LocalShell.getShell().command(renameTab).execute();

        var run = CommandBuilder.of().add("qdbus", "org.kde.yakuake", "/yakuake/sessions", "runCommandInTerminal", index).addFile(configuration.single().getScriptFile());
        LocalShell.getShell().command(run).execute();
    }

    @Override
    public String getExecutable() {
        return "yakuake";
    }

    @Override
    public boolean detach() {
        return false;
    }

    @Override
    public String getId() {
        return "app.yakuake";
    }
}
