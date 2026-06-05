package io.xpipe.app.terminal;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.webtop.WebtopApp;

public interface GhosttyTerminalType extends ExternalTerminalType, TrackableTerminalType {

    ExternalTerminalType GHOSTTY_LINUX = new Linux();
    ExternalTerminalType GHOSTTY_MACOS = new MacOs();

    @Override
    default String getWebsite() {
        return "https://ghostty.org";
    }

    @Override
    default boolean useColoredTitle() {
        return true;
    }

    class Linux implements GhosttyTerminalType, ExternalApplicationType.PathApplication {

        @Override
        public WebtopApp getRequiredWebtopApp() {
            return WebtopApp.GHOSTTY;
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW;
        }

        @Override
        public boolean isRecommended() {
            return AppPrefs.get().terminalMultiplexer().getValue() != null;
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            var builder =
                    CommandBuilder.of().add("-e").addFile(configuration.single().getScriptFile());
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
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            CommandBuilder b = configuration.isPreferTabs()
                    ? CommandBuilder.of()
                            .add("open", "-a")
                            .addQuoted(getApplicationName())
                            .addFile(configuration.single().getScriptFile())
                    : CommandBuilder.of()
                            .add("open", "-n", "-a")
                            .addQuoted(getApplicationName())
                            .add("--args", "-e")
                            .add(configuration.single().getDialectLaunchCommand());
            LocalShell.getShell().command(b).execute();
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
