package io.xpipe.app.terminal;

import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.*;
import io.xpipe.app.util.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public interface WarpTerminalType extends ExternalTerminalType, TrackableTerminalType {

    WarpTerminalType WINDOWS = new Windows();
    WarpTerminalType LINUX = new Linux();
    WarpTerminalType MACOS = new MacOs();

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

    class Windows implements WarpTerminalType {

        @Override
        public int getProcessHierarchyOffset() {
            return 0;
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

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            try (var sc = LocalShell.getShell().start()) {
                var command = configuration.getScriptDialect().getSetEnvironmentVariableCommand("PSModulePath", "")
                        + "\n"
                        + configuration
                                .getScriptDialect()
                                .runScriptCommand(
                                        sc, configuration.getScriptFile().toString());

                // Move to subdir as Warp tries to index the parent dir, which would be temp in this case
                var scriptFile = ScriptHelper.createExecScript(configuration.getScriptDialect(), sc, command);
                var movedScriptFile =
                        AppSystemInfo.ofCurrent().getTemp().resolve("warp").resolve(scriptFile.getFileName());
                Files.createDirectories(movedScriptFile.getParent());
                Files.move(scriptFile.asLocalPath(), movedScriptFile);

                var scriptArg = URLEncoder.encode(movedScriptFile.toString(), StandardCharsets.UTF_8);
                if (!configuration.isPreferTabs()) {
                    DesktopHelper.openUrl("warp://action/new_window?path=" + scriptArg);
                } else {
                    DesktopHelper.openUrl("warp://action/new_tab?path=" + scriptArg);
                }
            }
        }
    }

    class Linux implements WarpTerminalType {

        @Override
        public int getProcessHierarchyOffset() {
            return 2;
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

        @Override
        public void launch(TerminalLaunchConfiguration configuration) {
            if (!configuration.isPreferTabs()) {
                DesktopHelper.openUrl("warp://action/new_window?path=" + configuration.getScriptFile());
            } else {
                DesktopHelper.openUrl("warp://action/new_tab?path=" + configuration.getScriptFile());
            }
        }
    }

    class MacOs implements ExternalApplicationType.MacApplication, WarpTerminalType {

        @Override
        public int getProcessHierarchyOffset() {
            return 2;
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.TABBED;
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
        public String getApplicationName() {
            return "Warp";
        }

        @Override
        public String getId() {
            return "app.warp";
        }
    }
}
