package io.xpipe.app.terminal;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;

import java.io.IOException;
import java.nio.file.Files;

public class FootTerminalType implements ExternalTerminalType, ExternalApplicationType.LinuxApplication {

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW;
    }

    @Override
    public String getWebsite() {
        return "https://codeberg.org/dnkl/foot";
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
    public boolean supportsEscapes() {
        return false;
    }

    @Override
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        var toExecute = CommandBuilder.of()
                .add("--title")
                .addQuoted(configuration.getColoredTitle())
                .addFile(configuration.getScriptFile());
        launch(toExecute);
    }

    @Override
    public String getFlatpakId() throws Exception {
        return "page.codeberg.dnkl.foot";
    }

    @Override
    public String getExecutable() {
        return "foot";
    }

    @Override
    public boolean detach() {
        return true;
    }

    @Override
    public String getId() {
        return "app.foot";
    }
}
