package io.xpipe.app.terminal;

import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.WindowsRegistry;
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
            DesktopHelper.openUrl("warp://action/new_tab?path=" + configuration.getScriptFile());
        }

        @Override
        public boolean isAvailable() {
            return WindowsRegistry.local().keyExists(WindowsRegistry.HKEY_CURRENT_USER, "Software\\Classes\\warp");
        }

        @Override
        public String getId() {
            return "app.warp";
        }
    }


    class Linux implements WarpTerminalType {

        @Override
        public int getProcessHierarchyOffset() {
            return 2;
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            DesktopHelper.openUrl("warp://action/new_tab?path=" + configuration.getScriptFile());
        }

        @Override
        public boolean isAvailable() {
            return Files.exists(Path.of("/opt/warpdotdev"));
        }

        @Override
        public String getId() {
            return "app.warp";
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

    }

    @Override
    default TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.TABBED;
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
