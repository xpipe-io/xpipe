package io.xpipe.app.terminal;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.util.FlatpakCache;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.OsType;

import java.nio.file.Path;
import java.util.Optional;

public interface WezTerminalType extends ExternalTerminalType, TrackableTerminalType {

    ExternalTerminalType WEZTERM_WINDOWS = new Windows();
    ExternalTerminalType WEZTERM_LINUX = new Linux();
    ExternalTerminalType WEZTERM_MAC_OS = new MacOs();

    @Override
    default String getWebsite() {
        return "https://wezfurlong.org/wezterm/index.html";
    }

    @Override
    default boolean isRecommended() {
        return OsType.ofLocal() != OsType.WINDOWS;
    }

    @Override
    default boolean useColoredTitle() {
        return true;
    }

    class Windows implements ExternalApplicationType.WindowsType, ExternalTerminalType, WezTerminalType {

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW;
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            launch(CommandBuilder.of().add("start").add(configuration.single().getDialectLaunchCommand()));
        }

        @Override
        public boolean detach() {
            return false;
        }

        @Override
        public String getExecutable() {
            return "wezterm-gui";
        }

        @Override
        public Optional<Path> determineInstallation() {
            try {
                var foundKey = WindowsRegistry.local()
                        .findKeyForEqualValueMatchRecursive(
                                WindowsRegistry.HKEY_LOCAL_MACHINE,
                                "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
                                "http://wezfurlong.org/wezterm");
                if (foundKey.isPresent()) {
                    var installKey = WindowsRegistry.local()
                            .readStringValueIfPresent(
                                    foundKey.get().getHkey(), foundKey.get().getKey(), "InstallLocation");
                    if (installKey.isPresent()) {
                        return installKey.map(p -> p + "\\wezterm-gui.exe").map(Path::of);
                    }
                }
            } catch (Exception ex) {
                ErrorEventFactory.fromThrowable(ex).omit().handle();
            }

            try {
                if (CommandSupport.isInLocalPath("wezterm")) {
                    return Optional.of(Path.of("wezterm-gui"));
                }
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).omit().handle();
            }

            return Optional.empty();
        }

        @Override
        public String getId() {
            return "app.wezterm";
        }
    }

    class Linux implements ExternalApplicationType, WezTerminalType {

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            boolean runGui = true;
            var flatpak = FlatpakCache.getApp("org.wezfurlong.wezterm");
            if (configuration.isPreferTabs()) {
                CommandBuilder base;
                if (CommandSupport.isInLocalPath("wezterm")) {
                    base = CommandBuilder.of().addFile("wezterm");
                } else {
                    if (flatpak.isPresent()) {
                        base = FlatpakCache.runCommand("org.wezfurlong.wezterm");
                    } else {
                        base = CommandBuilder.of().addFile("wezterm");
                    }
                }

                runGui = !LocalShell.getShell()
                        .command(CommandBuilder.of()
                                .add(base)
                                .add("cli", "spawn")
                                .addFile(configuration.single().getScriptFile()))
                        .executeAndCheck();
            }

            if (runGui) {
                CommandBuilder base;
                if (CommandSupport.isInLocalPath("wezterm-gui")) {
                    base = CommandBuilder.of().addFile("wezterm-gui");
                } else {
                    if (flatpak.isPresent()) {
                        base = FlatpakCache.runCommand("org.wezfurlong.wezterm");
                    } else {
                        base = CommandBuilder.of().addFile("wezterm-gui");
                    }
                }
                ExternalApplicationHelper.startAsync(
                        CommandBuilder.of().add(base).add("start").addFile(configuration.single().getScriptFile()));
            }
        }

        public boolean isAvailable() {
            try {
                return CommandSupport.isInLocalPath("wezterm") && CommandSupport.isInLocalPath("wezterm-gui");
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).omit().handle();
                return false;
            }
        }

        @Override
        public String getId() {
            return "app.wezterm";
        }
    }

    class MacOs implements ExternalApplicationType.MacApplication, WezTerminalType {

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            try (var sc = LocalShell.getShell()) {
                var pathOut = sc.command(String.format(
                                "mdfind -name '%s' -onlyin /Applications -onlyin ~/Applications -onlyin /System/Applications 2>/dev/null",
                                getApplicationName()))
                        .readStdoutOrThrow();
                var path = Path.of(pathOut);

                boolean runGui = true;
                if (configuration.isPreferTabs()) {
                    runGui = !sc.command(CommandBuilder.of()
                                    .addFile(path.resolve("Contents")
                                            .resolve("MacOS")
                                            .resolve("wezterm")
                                            .toString())
                                    .add("cli", "spawn", "--pane-id", "0")
                                    .addFile(configuration.single().getScriptFile()))
                            .executeAndCheck();
                }
                if (runGui) {
                    ExternalApplicationHelper.startAsync(CommandBuilder.of()
                            .addFile(path.resolve("Contents")
                                    .resolve("MacOS")
                                    .resolve("wezterm-gui")
                                    .toString())
                            .add("start")
                            .addFile(configuration.single().getScriptFile()));
                }
            }
        }

        @Override
        public String getApplicationName() {
            return "WezTerm";
        }

        @Override
        public String getId() {
            return "app.wezterm";
        }
    }
}
