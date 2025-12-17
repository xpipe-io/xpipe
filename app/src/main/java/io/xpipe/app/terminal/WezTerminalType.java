package io.xpipe.app.terminal;

import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.util.FlatpakCache;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

public interface WezTerminalType extends ExternalTerminalType, TrackableTerminalType {

    ExternalTerminalType WEZTERM_WINDOWS = new Windows();
    ExternalTerminalType WEZTERM_LINUX = new Linux();
    ExternalTerminalType WEZTERM_MAC_OS = new MacOs();

    @Override
    default TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
    }

    @Override
    default String getWebsite() {
        return "https://wezfurlong.org/wezterm/index.html";
    }

    @Override
    default boolean isRecommended() {
        return true;
    }

    @Override
    default boolean supportsSplitView() {
        return true;
    }

    @Override
    default boolean useColoredTitle() {
        return true;
    }

    default Path getSocketDir() {
        return AppSystemInfo.ofCurrent().getUserHome().resolve(".local", "share", "wezterm");
    }

    default Optional<Path> waitForInstanceStart(int count) {
        Path dir = getSocketDir();
        if (!Files.exists(dir)) {
            return Optional.empty();
        }

        for (int i = 0; i < count; i++) {
            ThreadHelper.sleep(100);
            var active = getActiveSocket();
            if (active.isPresent()) {
                return active;
            }
        }

        return Optional.empty();
    }

    default Optional<Path> getActiveSocket() {
        Path dir = getSocketDir();
        if (!Files.exists(dir)) {
            return Optional.empty();
        }

        try (var stream = Files.list(dir)) {
            var files = stream.sorted(Comparator.<Path, Instant>comparing(path -> {
                try {
                    return Files.getLastModifiedTime(path).toInstant();
                } catch (IOException e) {
                    return Instant.MIN;
                }
            }).reversed()).toList();
            for (Path file : files) {
                if (file.getFileName().toString().contains("gui-sock")) {
                    try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
                        if (channel.connect(UnixDomainSocketAddress.of(file))) {
                            if (channel.isConnected()) {
                                return Optional.of(file);
                            }
                        }
                    } catch (IOException ignored) {}
                }
            }
        } catch (IOException ignored) {}

        return Optional.empty();
    }

    @Override
    default void launch(TerminalLaunchConfiguration configuration) throws Exception {
        var base = getWeztermCommandBase();

        var activeSocket = waitForInstanceStart(1);
        // Always start a new window for split panes as we can't find the pane index to start with
        if (activeSocket.isEmpty() || configuration.getPanes().size() > 1) {
            var command = CommandBuilder.of()
                    .add(base).add("start", "--always-new-process").add(configuration.getPanes().getFirst().getDialectLaunchCommand());
            ExternalApplicationHelper.startAsync(command);
        } else {
            var command = CommandBuilder.of()
                    .add(base).add("cli", "spawn")
                    .add(configuration.getPanes().getFirst().getDialectLaunchCommand());
            command.fixedEnvironment("WEZTERM_UNIX_SOCKET", activeSocket.get().toString());
            LocalShell.getShell().command(command)
                    .withWorkingDirectory(FilePath.of(getSocketDir())).execute();
        }

        if (configuration.getPanes().size() > 1) {
            activeSocket = waitForInstanceStart(50);
            if (activeSocket.isEmpty()) {
                return;
            }

            var direction = AppPrefs.get().terminalSplitStrategy().getValue();
            var directionIterator = direction.iterator();
            for (int i = 1; i < configuration.getPanes().size(); i++) {
                LocalShell.getShell()
                        .command(CommandBuilder.of()
                                .add(base)
                                .add("cli", "split-pane")
                                .addIf(directionIterator.getSplitDirection() == TerminalSplitStrategy.SplitDirection.HORIZONTAL, "--horizontal")
                                .addIf(directionIterator.getSplitDirection() == TerminalSplitStrategy.SplitDirection.VERTICAL, "--bottom")
                                .add("--pane-id", "" + directionIterator.getTargetPaneIndex())
                                .add("--percent", "50")
                                .add(configuration.getPanes().get(i).getDialectLaunchCommand())
                                .fixedEnvironment("WEZTERM_UNIX_SOCKET", activeSocket.get().toString()))
                        .withWorkingDirectory(FilePath.of(getSocketDir()))
                        .execute();
                directionIterator.next();
            }
        }
    }

    CommandBuilder getWeztermCommandBase() throws Exception;

    class Windows implements ExternalApplicationType.WindowsType, ExternalTerminalType, WezTerminalType {

        @Override
        public CommandBuilder getWeztermCommandBase() {
            return CommandBuilder.of().addFile(findExecutable());
        }

        @Override
        public boolean detach() {
            return true;
        }

        @Override
        public String getExecutable() {
            return "wezterm";
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
                        return installKey.map(p -> p + "\\wezterm.exe").map(Path::of);
                    }
                }
            } catch (Exception ex) {
                ErrorEventFactory.fromThrowable(ex).omit().handle();
            }

            try {
                if (CommandSupport.isInLocalPath("wezterm")) {
                    return Optional.of(Path.of("wezterm"));
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

    class Linux implements ExternalApplicationType.LinuxApplication, WezTerminalType {

        @Override
        public CommandBuilder getWeztermCommandBase() throws Exception {
            return getCommandBase();
        }

        @Override
        public String getExecutable() {
            return "wezterm";
        }

        @Override
        public boolean detach() {
            return true;
        }

        public boolean isAvailable() {
            try {
                return CommandSupport.isInLocalPath("wezterm");
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).omit().handle();
                return false;
            }
        }

        @Override
        public String getId() {
            return "app.wezterm";
        }

        @Override
        public String getFlatpakId() throws Exception {
            return "org.wezfurlong.wezterm";
        }
    }

    class MacOs implements ExternalApplicationType.MacApplication, WezTerminalType {

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
        public CommandBuilder getWeztermCommandBase() throws Exception {
            try (var sc = LocalShell.getShell()) {
                var pathOut = sc.command(
                        String.format("mdfind -name '%s' -onlyin /Applications -onlyin ~/Applications -onlyin /System/Applications 2>/dev/null",
                                getApplicationName())).readStdoutOrThrow();
                var path = Path.of(pathOut);
                return CommandBuilder.of().addFile(path.resolve("Contents").resolve("MacOS").resolve("wezterm"));
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
