package io.xpipe.app.terminal;

import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.process.TerminalInitFunction;

import java.nio.file.Path;
import java.util.Optional;

public interface TabbyTerminalType extends ExternalTerminalType {

    ExternalTerminalType TABBY_WINDOWS = new Windows();
    ExternalTerminalType TABBY_MAC_OS = new MacOs();

    @Override
    default boolean supportsTabs() {
        return true;
    }

    @Override
    default String getWebsite() {
        return "https://tabby.sh";
    }

    @Override
    default boolean isRecommended() {
        return true;
    }

    @Override
    default boolean supportsColoredTitle() {
        return true;
    }

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
        return null;
    }

    class Windows extends ExternalTerminalType.WindowsType implements TabbyTerminalType {

        public Windows() {
            super("app.tabby", "Tabby.exe");
        }

        @Override
        protected void execute(Path file, LaunchConfiguration configuration) throws Exception {
            // Tabby has a very weird handling of output, even detaching with start does not prevent it from printing
            if (configuration.getScriptDialect().equals(ShellDialects.CMD)) {
                // It also freezes with any other input than .bat files, why?
                LocalShell.getShell()
                        .executeSimpleCommand(CommandBuilder.of()
                                .addFile(file.toString())
                                .add("run")
                                .addFile(configuration.getScriptFile())
                                .discardOutput());
            }

            // This is probably not going to work as it does not launch a bat file
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .addFile(file.toString())
                            .add("run")
                            .add(configuration.getDialectLaunchCommand())
                            .discardOutput());
        }

        @Override
        protected Optional<Path> determineInstallation() {
            var perUser = WindowsRegistry.readString(
                            WindowsRegistry.HKEY_CURRENT_USER,
                            "SOFTWARE\\71445fac-d6ef-5436-9da7-5a323762d7f5",
                            "InstallLocation")
                    .map(p -> p + "\\Tabby.exe")
                    .map(Path::of);
            if (perUser.isPresent()) {
                return perUser;
            }

            var systemWide = WindowsRegistry.readString(
                            WindowsRegistry.HKEY_LOCAL_MACHINE,
                            "SOFTWARE\\71445fac-d6ef-5436-9da7-5a323762d7f5",
                            "InstallLocation")
                    .map(p -> p + "\\Tabby.exe")
                    .map(Path::of);
            return systemWide;
        }
    }

    class MacOs extends MacOsType implements TabbyTerminalType {

        public MacOs() {
            super("app.tabby", "Tabby");
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("open", "-a")
                            .addQuoted("Tabby.app")
                            .add("-n", "--args", "run")
                            .addFile(configuration.getScriptFile()));
        }
    }
}
