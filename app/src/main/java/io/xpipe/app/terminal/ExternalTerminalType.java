package io.xpipe.app.terminal;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.FailableFunction;

import lombok.Getter;
import lombok.Value;
import lombok.With;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

public interface ExternalTerminalType extends PrefsChoiceValue {

    ExternalTerminalType CMD = new SimplePathType("app.cmd", "cmd.exe", true) {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            if (configuration.getScriptDialect().equals(ShellDialects.CMD)) {
                return CommandBuilder.of().add("/c").addFile(configuration.getScriptFile());
            }

            return CommandBuilder.of().add("/c").add(configuration.getDialectLaunchCommand());
        }
    };

    ExternalTerminalType POWERSHELL = new SimplePathType("app.powershell", "powershell", true) {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            if (configuration.getScriptDialect().equals(ShellDialects.POWERSHELL)) {
                return CommandBuilder.of()
                        .add("-ExecutionPolicy", "Bypass")
                        .add("-File")
                        .addFile(configuration.getScriptFile());
            }

            return CommandBuilder.of()
                    .add("-ExecutionPolicy", "Bypass")
                    .add("-EncodedCommand")
                    .add(sc -> {
                        var base64 = Base64.getEncoder()
                                .encodeToString(configuration
                                        .getDialectLaunchCommand()
                                        .buildBase(sc)
                                        .getBytes(StandardCharsets.UTF_16LE));
                        return "\"" + base64 + "\"";
                    });
        }
    };

    ExternalTerminalType PWSH = new SimplePathType("app.pwsh", "pwsh", true) {

        @Override
        public String getWebsite() {
            return "https://learn.microsoft.com/en-us/powershell/scripting/install/installing-powershell?view=powershell-7.4";
        }

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-ExecutionPolicy", "Bypass")
                    .add("-EncodedCommand")
                    .add(sc -> {
                        // Fix for https://github.com/PowerShell/PowerShell/issues/18530#issuecomment-1325691850
                        var c = "$env:PSModulePath=\"\";"
                                + configuration.getDialectLaunchCommand().buildBase(sc);
                        var base64 = Base64.getEncoder().encodeToString(c.getBytes(StandardCharsets.UTF_16LE));
                        return "\"" + base64 + "\"";
                    });
        }
    };
    ExternalTerminalType GNOME_TERMINAL = new PathCheckType("app.gnomeTerminal", "gnome-terminal", true) {
        @Override
        public String getWebsite() {
            return "https://help.gnome.org/users/gnome-terminal/stable/";
        }

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            try (ShellControl pc = LocalShell.getShell()) {
                CommandSupport.isInPathOrThrow(
                        pc, executable, toTranslatedString().getValue(), null);

                var toExecute = CommandBuilder.of()
                        .add(executable, "-v", "--title")
                        .addQuoted(configuration.getColoredTitle())
                        .add("--")
                        .addFile(configuration.getScriptFile())
                        // In order to fix this bug which also affects us:
                        // https://askubuntu.com/questions/1148475/launching-gnome-terminal-from-vscode
                        .envrironment("GNOME_TERMINAL_SCREEN", sc -> "");
                pc.executeSimpleCommand(toExecute);
            }
        }

        @Override
        public FailableFunction<LaunchConfiguration, String, Exception> remoteLaunchCommand(
                ShellDialect systemDialect) {
            return launchConfiguration -> {
                var toExecute = CommandBuilder.of()
                        .add(executable, "-v", "--title")
                        .addQuoted(launchConfiguration.getColoredTitle())
                        .add("--")
                        .addFile(launchConfiguration.getScriptFile());
                return toExecute.buildSimple();
            };
        }
    };
    ExternalTerminalType KONSOLE = new SimplePathType("app.konsole", "konsole", true) {

        @Override
        public String getWebsite() {
            return "https://konsole.kde.org/download.html";
        }

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            // Note for later: When debugging konsole launches, it will always open as a child process of
            // IntelliJ/XPipe even though we try to detach it.
            // This is not the case for production where it works as expected
            return CommandBuilder.of().add("--new-tab", "-e").addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType XFCE = new SimplePathType("app.xfce", "xfce4-terminal", true) {
        @Override
        public String getWebsite() {
            return "https://docs.xfce.org/apps/terminal/start";
        }

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("--tab", "--title")
                    .addQuoted(configuration.getColoredTitle())
                    .add("--command")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType ELEMENTARY = new SimplePathType("app.elementaryTerminal", "io.elementary.terminal", true) {

        @Override
        public String getWebsite() {
            return "https://github.com/elementary/terminal";
        }

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of().add("--new-tab").add("-e").addFile(configuration.getColoredTitle());
        }
    };
    ExternalTerminalType TILIX = new SimplePathType("app.tilix", "tilix", true) {
        @Override
        public String getWebsite() {
            return "https://gnunn1.github.io/tilix-web/";
        }

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-t")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType TERMINATOR = new SimplePathType("app.terminator", "terminator", true) {
        @Override
        public String getWebsite() {
            return "https://gnome-terminator.org/";
        }

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-e")
                    .addFile(configuration.getScriptFile())
                    .add("-T")
                    .addQuoted(configuration.getColoredTitle())
                    .add("--new-tab");
        }
    };
    ExternalTerminalType TERMINOLOGY = new SimplePathType("app.terminology", "terminology", true) {
        @Override
        public String getWebsite() {
            return "https://github.com/borisfaure/terminology";
        }

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-T")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-2")
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType GUAKE = new SimplePathType("app.guake", "guake", true) {
        @Override
        public String getWebsite() {
            return "https://github.com/Guake/guake";
        }

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-n", "~")
                    .add("-r")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType TILDA = new SimplePathType("app.tilda", "tilda", true) {
        @Override
        public String getWebsite() {
            return "https://github.com/lanoxx/tilda";
        }

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of().add("-c").addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType XTERM = new SimplePathType("app.xterm", "xterm", true) {
        @Override
        public String getWebsite() {
            return "https://invisible-island.net/xterm/";
        }

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-title")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType DEEPIN_TERMINAL = new SimplePathType("app.deepinTerminal", "deepin-terminal", true) {
        @Override
        public String getWebsite() {
            return "https://www.deepin.org/en/original/deepin-terminal/";
        }

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of().add("-C").addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType Q_TERMINAL = new SimplePathType("app.qTerminal", "qterminal", true) {
        @Override
        public String getWebsite() {
            return "https://github.com/lxqt/qterminal";
        }

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of().add("-e").addQuoted(configuration.getColoredTitle());
        }
    };
    ExternalTerminalType MACOS_TERMINAL = new MacOsType("app.macosTerminal", "Terminal") {
        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            try (ShellControl pc = LocalShell.getShell()) {
                var suffix = "\"" + configuration.getScriptFile().toString().replaceAll("\"", "\\\\\"") + "\"";
                pc.osascriptCommand(String.format(
                                """
                                activate application "Terminal"
                                delay 1
                                tell app "Terminal" to do script %s
                                """,
                                suffix))
                        .execute();
            }
        }
    };
    ExternalTerminalType ITERM2 = new MacOsType("app.iterm2", "iTerm") {
        @Override
        public String getWebsite() {
            return "https://iterm2.com/";
        }

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            try (ShellControl pc = LocalShell.getShell()) {
                pc.osascriptCommand(String.format(
                                """
                                if application "iTerm" is not running then
                                    launch application "iTerm"
                                    delay 1
                                    tell application "iTerm"
                                        tell current tab of current window
                                            close
                                        end tell
                                    end tell
                                end if
                                tell application "iTerm"
                                    activate
                                    create window with default profile command "%s"
                                end tell
                                """,
                                configuration.getScriptFile().toString().replaceAll("\"", "\\\\\"")))
                        .execute();
            }
        }
    };
    ExternalTerminalType WARP = new MacOsType("app.warp", "Warp") {

        @Override
        public String getWebsite() {
            return "https://www.warp.dev/";
        }

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        public boolean shouldClear() {
            return false;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("open", "-a")
                            .addQuoted("Warp.app")
                            .addFile(configuration.getScriptFile()));
        }

        @Override
        public FailableFunction<LaunchConfiguration, String, Exception> remoteLaunchCommand(
                ShellDialect systemDialect) {
            return launchConfiguration -> {
                var toExecute = CommandBuilder.of()
                        .add("open", "-a")
                        .addQuoted("Warp.app")
                        .addFile(launchConfiguration.getScriptFile());
                return toExecute.buildSimple();
            };
        }

        @Override
        public TerminalInitFunction additionalInitCommands() {
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
    };
    ExternalTerminalType CUSTOM = new CustomTerminalType();
    List<ExternalTerminalType> WINDOWS_TERMINALS = List.of(
            TabbyTerminalType.TABBY_WINDOWS,
            AlacrittyTerminalType.ALACRITTY_WINDOWS,
            WezTerminalType.WEZTERM_WINDOWS,
            WindowsTerminalType.WINDOWS_TERMINAL_PREVIEW,
            WindowsTerminalType.WINDOWS_TERMINAL,
            CMD,
            PWSH,
            POWERSHELL);
    List<ExternalTerminalType> LINUX_TERMINALS = List.of(
            WezTerminalType.WEZTERM_LINUX,
            KONSOLE,
            XFCE,
            ELEMENTARY,
            GNOME_TERMINAL,
            TILIX,
            TERMINATOR,
            KittyTerminalType.KITTY_LINUX,
            TERMINOLOGY,
            GUAKE,
            AlacrittyTerminalType.ALACRITTY_LINUX,
            TILDA,
            XTERM,
            DEEPIN_TERMINAL,
            Q_TERMINAL);
    List<ExternalTerminalType> MACOS_TERMINALS = List.of(
            ITERM2,
            TabbyTerminalType.TABBY_MAC_OS,
            AlacrittyTerminalType.ALACRITTY_MAC_OS,
            KittyTerminalType.KITTY_MACOS,
            WARP,
            WezTerminalType.WEZTERM_MAC_OS,
            MACOS_TERMINAL);

    List<ExternalTerminalType> ALL = getTypes(OsType.getLocal(), false, true);

    List<ExternalTerminalType> ALL_ON_ALL_PLATFORMS = getTypes(null, false, true);

    static List<ExternalTerminalType> getTypes(OsType osType, boolean remote, boolean custom) {
        var all = new ArrayList<ExternalTerminalType>();
        if (osType == null || osType.equals(OsType.WINDOWS)) {
            all.addAll(WINDOWS_TERMINALS);
        }
        if (osType == null || osType.equals(OsType.LINUX)) {
            all.addAll(LINUX_TERMINALS);
        }
        if (osType == null || osType.equals(OsType.MACOS)) {
            all.addAll(MACOS_TERMINALS);
        }
        if (remote) {
            all.removeIf(externalTerminalType -> externalTerminalType.remoteLaunchCommand(null) == null);
        }
        // Prefer recommended
        all.sort(Comparator.comparingInt(o -> (o.isRecommended() ? -1 : 0)));
        if (custom) {
            all.add(CUSTOM);
        }
        return all;
    }

    static ExternalTerminalType determineDefault(ExternalTerminalType existing) {
        // Check for incompatibility with fallback shell
        if (ExternalTerminalType.CMD.equals(existing)
                && !ProcessControlProvider.get().getEffectiveLocalDialect().equals(ShellDialects.CMD)) {
            return ExternalTerminalType.POWERSHELL;
        }

        if (existing != null) {
            return existing;
        }

        return ALL.stream()
                .filter(externalTerminalType -> !externalTerminalType.equals(CUSTOM))
                .filter(terminalType -> terminalType.isAvailable())
                .findFirst()
                .orElse(null);
    }

    default TerminalInitFunction additionalInitCommands() {
        return TerminalInitFunction.none();
    }

    boolean supportsTabs();

    default String getWebsite() {
        return null;
    }

    boolean isRecommended();

    boolean supportsColoredTitle();

    default boolean shouldClear() {
        return true;
    }

    default void launch(LaunchConfiguration configuration) throws Exception {}

    default FailableFunction<LaunchConfiguration, String, Exception> remoteLaunchCommand(ShellDialect systemDialect) {
        return null;
    }

    abstract class WindowsType extends ExternalApplicationType.WindowsType implements ExternalTerminalType {

        public WindowsType(String id, String executable) {
            super(id, executable);
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var location = determineFromPath();
            if (location.isEmpty()) {
                location = determineInstallation();
                if (location.isEmpty()) {
                    throw new IOException("Unable to find installation of "
                            + toTranslatedString().getValue());
                }
            }

            execute(location.get(), configuration);
        }

        protected abstract void execute(Path file, LaunchConfiguration configuration) throws Exception;
    }

    @Value
    class LaunchConfiguration {
        DataStoreColor color;
        String coloredTitle;
        String cleanTitle;

        @With
        FilePath scriptFile;

        ShellDialect scriptDialect;

        public CommandBuilder getDialectLaunchCommand() {
            var open = scriptDialect.getOpenScriptCommand(scriptFile.toString());
            return open;
        }
    }

    abstract class MacOsType extends ExternalApplicationType.MacApplication implements ExternalTerminalType {

        public MacOsType(String id, String applicationName) {
            super(id, applicationName);
        }
    }

    @Getter
    abstract class PathCheckType extends ExternalApplicationType.PathApplication implements ExternalTerminalType {

        public PathCheckType(String id, String executable, boolean explicitAsync) {
            super(id, executable, explicitAsync);
        }
    }

    @Getter
    abstract class SimplePathType extends PathCheckType {

        public SimplePathType(String id, String executable, boolean explicitAsync) {
            super(id, executable, explicitAsync);
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var args = toCommand(configuration);
            launch(configuration.getColoredTitle(), args);
        }

        @Override
        public FailableFunction<LaunchConfiguration, String, Exception> remoteLaunchCommand(
                ShellDialect systemDialect) {
            return launchConfiguration -> {
                var args = toCommand(launchConfiguration);
                args.add(0, executable);
                if (explicitlyAsync) {
                    args = systemDialect.launchAsnyc(args);
                }
                return args.buildSimple();
            };
        }

        protected abstract CommandBuilder toCommand(LaunchConfiguration configuration) throws Exception;
    }
}
