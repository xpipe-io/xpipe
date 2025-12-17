package io.xpipe.app.terminal;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;

public interface GhosttyTerminalType extends ExternalTerminalType, TrackableTerminalType {

    ExternalTerminalType GHOSTTY_LINUX = new Linux();
    ExternalTerminalType GHOSTTY_MACOS = new MacOs();

    @Override
    default TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW;
    }

    @Override
    default String getWebsite() {
        return "https://ghostty.org";
    }

    @Override
    default boolean isRecommended() {
        return AppPrefs.get().terminalMultiplexer().getValue() != null;
    }

    @Override
    default boolean useColoredTitle() {
        return true;
    }

    class Linux implements GhosttyTerminalType, ExternalApplicationType.PathApplication {

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            var builder = CommandBuilder.of().add("-e").addFile(configuration.single().getScriptFile());
            launch(builder);
        }

        @Override
        public String getId() {
            return "app.ghostty";
        }

        @Override
        public String getExecutable() {
            return "ghostty";
        }

        @Override
        public boolean detach() {
            return true;
        }
    }

    class MacOs implements ExternalApplicationType.MacApplication, GhosttyTerminalType {

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("open", "-n", "-a")
                            .addQuoted(getApplicationName())
                            .add("--args", "-e")
                            .add(configuration.single().getDialectLaunchCommand()));
        }

        @Override
        public String getApplicationName() {
            return "Ghostty";
        }

        @Override
        public String getId() {
            return "app.ghostty";
        }
    }
}
