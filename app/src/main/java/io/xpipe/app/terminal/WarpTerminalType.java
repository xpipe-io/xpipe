package io.xpipe.app.terminal;

import io.xpipe.app.util.*;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.process.TerminalInitFunction;

import java.nio.file.Files;
import java.nio.file.Path;

public interface WarpTerminalType extends ExternalTerminalType, TrackableTerminalType {

    static WarpTerminalType WINDOWS = new Windows();
    static WarpTerminalType LINUX = new Linux();
    static WarpTerminalType MACOS = new MacOs();

    class Windows implements WarpTerminalType {

        @Override
        public int getProcessHierarchyOffset() {
            return 1;
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            try (var sc = LocalShell.getShell().start()) {
                var command = configuration.getScriptDialect().getSetEnvironmentVariableCommand("PSModulePath", "")
                        + "\n"
                        + configuration
                                .getScriptDialect()
                                .runScriptCommand(
                                        sc, configuration.getScriptFile().toString());
                var script = ScriptHelper.createExecScript(configuration.getScriptDialect(), sc, command);
                if (!configuration.isPreferTabs()) {
                    DesktopHelper.openUrl("warp://action/new_window?path=" + script);
                } else {
                    DesktopHelper.openUrl("warp://action/new_tab?path=" + script);
                }
            }
        }

        @Override
        public boolean isAvailable() {
            return WindowsRegistry.local().keyExists(WindowsRegistry.HKEY_CURRENT_USER, "Software\\Classes\\warp");
        }

        @Override
        public String getId() {
            return "app.warp";
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            // Warp always opens the new separate window, so we don't want to use it in the file browser for docking
            // Just say that we don't support new windows, that way it doesn't dock
            return TerminalOpenFormat.TABBED;
        }
    }

    class Linux implements WarpTerminalType {

        @Override
        public int getProcessHierarchyOffset() {
            return 2;
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            if (!configuration.isPreferTabs()) {
                DesktopHelper.openUrl("warp://action/new_window?path=" + configuration.getScriptFile());
            } else {
                DesktopHelper.openUrl("warp://action/new_tab?path=" + configuration.getScriptFile());
            }
        }

        @Override
        public boolean isAvailable() {
            return Files.exists(Path.of("/opt/warpdotdev"));
        }

        @Override
        public String getId() {
            return "app.warp";
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            // Warp always opens the new separate window, so we don't want to use it in the file browser for docking
            // Just say that we don't support new windows, that way it doesn't dock
            return TerminalOpenFormat.TABBED;
        }
    }

    class MacOs extends MacOsType implements WarpTerminalType {

        public MacOs() {
            super("app.warp", "Warp");
        }

        @Override
        public int getProcessHierarchyOffset() {
            return 2;
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("open", "-a")
                            .addQuoted("Warp.app")
                            .addFile(configuration.getScriptFile()));
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.TABBED;
        }
    }

    @Override
    default String getWebsite() {
        return "https://www.warp.dev/";
    }

    @Override
    default boolean isRecommended() {
        return true;
    }

    @Override
    default boolean useColoredTitle() {
        return true;
    }

    @Override
    default boolean shouldClear() {
        return false;
    }

    @Override
    default TerminalInitFunction additionalInitCommands() {
        return TerminalInitFunction.of(sc -> {
            if (sc.getShellDialect() == ShellDialects.ZSH) {
                return "printf '\\eP$f{\"hook\": \"SourcedRcFileForWarp\", \"value\": { \"shell\": \"zsh\"}}\\x9c'";
            }
            if (sc.getShellDialect() == ShellDialects.BASH) {
                return "printf '\\eP$f{\"hook\": \"SourcedRcFileForWarp\", \"value\": { \"shell\": \"bash\"}}\\x9c'";
            }
            if (sc.getShellDialect() == ShellDialects.FISH) {
                return "printf '\\eP$f{\"hook\": \"SourcedRcFileForWarp\", \"value\": { \"shell\": \"fish\"}}\\x9c'";
            }
            return null;
        });
    }
}
