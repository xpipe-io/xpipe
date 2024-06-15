package io.xpipe.app.terminal;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;

import java.nio.file.Path;
import java.util.Optional;

public interface WezTerminalType extends ExternalTerminalType {

    ExternalTerminalType WEZTERM_WINDOWS = new Windows();
    ExternalTerminalType WEZTERM_LINUX = new Linux();
    ExternalTerminalType WEZTERM_MAC_OS = new MacOs();

    @Override
    default boolean supportsTabs() {
        return false;
    }

    @Override
    default String getWebsite() {
        return "https://wezfurlong.org/wezterm/index.html";
    }

    @Override
    default boolean isRecommended() {
        return OsType.getLocal() != OsType.WINDOWS;
    }

    @Override
    default boolean supportsColoredTitle() {
        return true;
    }

    class Windows extends WindowsType implements WezTerminalType {

        public Windows() {
            super("app.wezterm", "wezterm-gui");
        }

        @Override
        protected void execute(Path file, LaunchConfiguration configuration) throws Exception {
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .addFile(file.toString())
                            .add("start")
                            .add(configuration.getDialectLaunchCommand()));
        }

        @Override
        protected Optional<Path> determineInstallation() {
            try {
                var foundKey = WindowsRegistry.local()
                        .findKeyForEqualValueMatchRecursive(
                                WindowsRegistry.HKEY_LOCAL_MACHINE,
                                "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
                                "http://wezfurlong.org/wezterm");
                if (foundKey.isPresent()) {
                    var installKey = WindowsRegistry.local()
                            .readValue(foundKey.get().getHkey(), foundKey.get().getKey(), "InstallLocation");
                    if (installKey.isPresent()) {
                        return installKey.map(p -> p + "\\wezterm-gui.exe").map(Path::of);
                    }
                }
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).omit().handle();
            }

            try (ShellControl pc = LocalShell.getShell()) {
                if (pc.executeSimpleBooleanCommand(pc.getShellDialect().getWhichCommand("wezterm-gui"))) {
                    return Optional.of(Path.of("wezterm-gui"));
                }
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
            }

            return Optional.empty();
        }
    }

    class Linux extends ExternalApplicationType implements WezTerminalType {

        public Linux() {
            super("app.wezterm");
        }

        public boolean isAvailable() {
            try (ShellControl pc = LocalShell.getShell()) {
                return pc.executeSimpleBooleanCommand(pc.getShellDialect().getWhichCommand("wezterm"))
                        && pc.executeSimpleBooleanCommand(pc.getShellDialect().getWhichCommand("wezterm-gui"));
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
                return false;
            }
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var spawn = LocalShell.getShell()
                    .command(CommandBuilder.of()
                            .addFile("wezterm")
                            .add("cli", "spawn")
                            .addFile(configuration.getScriptFile()))
                    .executeAndCheck();
            if (!spawn) {
                ExternalApplicationHelper.startAsync(
                        CommandBuilder.of().addFile("wezterm-gui").add("start").addFile(configuration.getScriptFile()));
            }
        }
    }

    class MacOs extends MacOsType implements WezTerminalType {

        public MacOs() {
            super("app.wezterm", "WezTerm");
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            try (var sc = LocalShell.getShell()) {
                var path = sc.command(String.format(
                                "mdfind -name '%s' -onlyin /Applications -onlyin ~/Applications -onlyin /System/Applications 2>/dev/null",
                                applicationName))
                        .readStdoutOrThrow();
                var spawn = sc.command(CommandBuilder.of()
                                .addFile(Path.of(path)
                                        .resolve("Contents")
                                        .resolve("MacOS")
                                        .resolve("wezterm")
                                        .toString())
                                .add("cli", "spawn", "--pane-id", "0")
                                .addFile(configuration.getScriptFile()))
                        .executeAndCheck();
                if (!spawn) {
                    ExternalApplicationHelper.startAsync(CommandBuilder.of()
                            .addFile(Path.of(path)
                                    .resolve("Contents")
                                    .resolve("MacOS")
                                    .resolve("wezterm-gui")
                                    .toString())
                            .add("start")
                            .addFile(configuration.getScriptFile()));
                }
            }
        }
    }
}
