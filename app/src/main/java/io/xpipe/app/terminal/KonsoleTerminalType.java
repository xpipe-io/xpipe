package io.xpipe.app.terminal;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;

import java.io.IOException;
import java.nio.file.Files;

public class KonsoleTerminalType implements ExternalTerminalType, ExternalApplicationType.LinuxApplication {

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
    }

    @Override
    public String getWebsite() {
        return "https://konsole.kde.org/download.html";
    }

    @Override
    public boolean isRecommended() {
        // We can manually fix the single process option in konsole
        return true;
    }

    @Override
    public boolean useColoredTitle() {
        return false;
    }

    @Override
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        configureSingleInstanceMode();
        // Note for later: When debugging konsole launches, it will always open as a child process of
        // IntelliJ/XPipe even though we try to detach it.
        // This is not the case for production where it works as expected
        var toExecute = CommandBuilder.of()
                .addIf(configuration.isPreferTabs(), "--new-tab")
                .add("-e")
                .addFile(configuration.getScriptFile());
        launch(toExecute);
    }

    private synchronized void configureSingleInstanceMode() {
        var cache = AppCache.getBoolean("konsoleInstanceOptionSet", false);
        if (cache) {
            return;
        }

        var config = AppSystemInfo.ofCurrent().getUserHome().resolve(".config", "konsolerc");
        if (!Files.exists(config)) {
            return;
        }

        try {
            var content = Files.readString(config);
            var contains = content.contains("UseSingleInstance=true");
            if (!contains) {
                var index = content.indexOf("[KonsoleWindow]");
                var augmented = index != -1
                        ? content.replace("[KonsoleWindow]", "[KonsoleWindow]\nUseSingleInstance=true")
                        : content + "\n\n[KonsoleWindow]\nUseSingleInstance=true\n";
                Files.writeString(config, augmented);
            }

            AppCache.update("konsoleInstanceOptionSet", true);
        } catch (IOException e) {
            ErrorEventFactory.fromThrowable(e).handle();
        }
    }

    @Override
    public String getFlatpakId() throws Exception {
        return "org.kde.konsole";
    }

    @Override
    public String getExecutable() {
        return "konsole";
    }

    @Override
    public boolean detach() {
        return true;
    }

    @Override
    public String getId() {
        return "app.konsole";
    }
}
