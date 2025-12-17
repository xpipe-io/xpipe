package io.xpipe.app.terminal;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.process.TerminalInitFunction;
import io.xpipe.app.util.WindowsRegistry;

import java.nio.file.Path;
import java.util.Optional;

public interface TabbyTerminalType extends ExternalTerminalType, TrackableTerminalType {

    ExternalTerminalType TABBY_WINDOWS = new Windows();
    ExternalTerminalType TABBY_MAC_OS = new MacOs();

    @Override
    default TerminalInitFunction additionalInitCommands() {
        //        return TerminalInitFunction.of(sc -> {
        //            if (sc.getShellDialect() == ShellDialects.ZSH) {
        //                return "export PS1=\"$PS1\\[\\e]1337;CurrentDir=\"'$(pwd)\\a\\]'";
        //            }
        //            if (sc.getShellDialect() == ShellDialects.BASH) {
        //                return "precmd () { echo -n \"\\x1b]1337;CurrentDir=$(pwd)\\x07\" }";
        //            }
        //            if (sc.getShellDialect() == ShellDialects.FISH) {
        //                return """
        //                       function __tabby_working_directory_reporting --on-event fish_prompt
        //                           echo -en "\\e]1337;CurrentDir=$PWD\\x7"
        //                       end
        //                       """;
        //            }
        //            return null;
        //        });
        return TerminalInitFunction.none();
    }

    @Override
    default TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.TABBED;
    }

    @Override
    default String getWebsite() {
        return "https://tabby.sh";
    }

    @Override
    default boolean useColoredTitle() {
        return true;
    }

    class Windows implements ExternalApplicationType.WindowsType, TabbyTerminalType {

        @Override
        public int getProcessHierarchyOffset() {
            return 1;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            var pane = configuration.single();
            // Tabby has a very weird handling of output, even detaching with start does not prevent it from printing
            if (pane.getScriptDialect() == ShellDialects.CMD) {
                // It also freezes with any other input than .bat files, why?
                launch(CommandBuilder.of()
                        .add("run")
                        .addFile(pane.getScriptFile())
                        .discardAllOutput());
            } else {
                // This is probably not going to work as it does not launch a bat file
                launch(CommandBuilder.of()
                        .add("run")
                        .add(sc -> pane
                                .getDialectLaunchCommand()
                                .buildFull(sc)
                                .replaceFirst("\\.exe", ""))
                        .discardAllOutput());
            }
        }

        @Override
        public boolean detach() {
            return true;
        }

        @Override
        public String getExecutable() {
            return "Tabby.exe";
        }

        @Override
        public Optional<Path> determineInstallation() {
            var perUser = WindowsRegistry.local()
                    .readStringValueIfPresent(
                            WindowsRegistry.HKEY_CURRENT_USER,
                            "SOFTWARE\\71445fac-d6ef-5436-9da7-5a323762d7f5",
                            "InstallLocation")
                    .map(p -> p + "\\Tabby.exe")
                    .map(Path::of);
            if (perUser.isPresent()) {
                return perUser;
            }

            var systemWide = WindowsRegistry.local()
                    .readStringValueIfPresent(
                            WindowsRegistry.HKEY_LOCAL_MACHINE,
                            "SOFTWARE\\71445fac-d6ef-5436-9da7-5a323762d7f5",
                            "InstallLocation")
                    .map(p -> p + "\\Tabby.exe")
                    .map(Path::of);
            return systemWide;
        }

        @Override
        public String getId() {
            return "app.tabby";
        }
    }

    class MacOs implements ExternalApplicationType.MacApplication, TabbyTerminalType {

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("open", "-a")
                            .addQuoted("Tabby.app")
                            .add("-n", "--args", "run")
                            .addFile(configuration.single().getScriptFile()));
        }

        @Override
        public String getApplicationName() {
            return "Tabby";
        }

        @Override
        public String getId() {
            return "app.tabby";
        }
    }
}
