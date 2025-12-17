package io.xpipe.app.terminal;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ShellControl;

public interface AlacrittyTerminalType extends ExternalTerminalType, TrackableTerminalType {

    ExternalTerminalType ALACRITTY_WINDOWS = new Windows();
    ExternalTerminalType ALACRITTY_LINUX = new Linux();
    ExternalTerminalType ALACRITTY_MAC_OS = new MacOs();

    @Override
    default TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW;
    }

    @Override
    default String getWebsite() {
        return "https://github.com/alacritty/alacritty";
    }

    @Override
    default boolean isRecommended() {
        return AppPrefs.get().terminalMultiplexer().getValue() != null;
    }

    @Override
    default boolean useColoredTitle() {
        return false;
    }

    class Windows implements ExternalApplicationType.PathApplication, ExternalTerminalType, AlacrittyTerminalType {

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            // Alacritty is bugged and will not accept arguments with spaces even if they are correctly passed/escaped
            // So this will not work when the script file has spaces
            var spaces = configuration.getPanes().getFirst().getScriptFile().toString().contains(" ");
            var scriptFile = spaces
                    ? configuration.single().getScriptFile().getFileName()
                    : configuration.single().getScriptFile().toString();
            var scriptOpenCommand = configuration.single().getScriptDialect().getOpenScriptCommand(scriptFile);
            var b = CommandBuilder.of().add("alacritty").add("-t")
                    .addQuoted(configuration.getCleanTitle()).add("-e").add(scriptOpenCommand);

            try (ShellControl sc = LocalShell.getShell()) {
                CommandSupport.isInPathOrThrow(sc, "alacritty");
                var command = sc.command(b);
                if (spaces) {
                        command.withWorkingDirectory(configuration.single().getScriptFile().getParent());
                }
                command.execute();
            }
        }

        @Override
        public String getExecutable() {
            return "alacritty";
        }

        @Override
        public boolean detach() {
            return false;
        }

        @Override
        public String getId() {
            return "app.alacritty";
        }
    }

    class Linux implements ExternalApplicationType.PathApplication, AlacrittyTerminalType {

        @Override
        public String getExecutable() {
            return "alacritty";
        }

        @Override
        public boolean detach() {
            return true;
        }

        @Override
        public String getId() {
            return "app.alacritty";
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            var b = CommandBuilder.of()
                    .add("-t")
                    .addQuoted(configuration.getCleanTitle())
                    .add("-e")
                    .addFile(configuration.single().getScriptFile());
            launch(b);
        }
    }

    class MacOs implements ExternalApplicationType.MacApplication, ExternalTerminalType, TrackableTerminalType {

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return null;
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
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("open", "-a")
                            .addQuoted("Alacritty.app")
                            .add("-n", "--args", "-t")
                            .addQuoted(configuration.getCleanTitle())
                            .add("-e")
                            .addFile(configuration.single().getScriptFile()));
        }

        @Override
        public String getId() {
            return "app.alacritty";
        }

        @Override
        public String getApplicationName() {
            return "Alacritty";
        }
    }
}
