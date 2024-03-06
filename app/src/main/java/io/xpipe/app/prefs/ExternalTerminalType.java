package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.util.*;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FileNames;
import lombok.Getter;
import lombok.Value;
import lombok.With;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public interface ExternalTerminalType extends PrefsChoiceValue {

    ExternalTerminalType CMD = new SimplePathType("app.cmd", "cmd.exe") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            if (configuration.getScriptDialect().equals(ShellDialects.CMD)) {
                return CommandBuilder.of().add("/c").add(configuration.getScriptFile());
            }

            return CommandBuilder.of().add("/c").add(configuration.getDialectLaunchCommand());
        }
    };

    ExternalTerminalType POWERSHELL = new SimplePathType("app.powershell", "powershell") {

        @Override
        public boolean supportsTabs() {
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
                        .add(configuration.getScriptFile());
            }

            return CommandBuilder.of().add("-Command").add(configuration.getDialectLaunchCommand());
        }
    };

    ExternalTerminalType PWSH = new SimplePathType("app.pwsh", "pwsh") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            if (configuration.getScriptDialect().equals(ShellDialects.POWERSHELL_CORE)) {
                return CommandBuilder.of()
                        .add("-ExecutionPolicy", "Bypass")
                        .add("-File")
                        .add(configuration.getScriptFile());
            }

            // Fix for https://github.com/PowerShell/PowerShell/issues/18530#issuecomment-1325691850
            var script = ScriptHelper.createLocalExecScript(
                    "set \"PSModulePath=\"\r\n& \"" + configuration.getScriptFile() + "\"");
            return CommandBuilder.of()
                    .add("-Command")
                    .add(configuration.withScriptFile(script).getDialectLaunchCommand());
        }
    };

    ExternalTerminalType WINDOWS_TERMINAL_PREVIEW = new ExternalTerminalType() {

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            // A weird behavior in Windows Terminal causes the trailing
            // backslash of a filepath to escape the closing quote in the title argument
            // So just remove that slash
            var fixedName = FileNames.removeTrailingSlash(configuration.getColoredTitle());
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .addFile(getPath().toString())
                            .add("-w", "1", "nt", "--title")
                            .addQuoted(fixedName)
                            .addFile(configuration.getScriptFile()));
        }

        private Path getPath() {
            var local = System.getenv("LOCALAPPDATA");
            return Path.of(local)
                    .resolve("Microsoft\\WindowsApps\\Microsoft.WindowsTerminalPreview_8wekyb3d8bbwe\\wt.exe");
        }

        @Override
        public boolean isAvailable() {
            return Files.exists(getPath());
        }

        @Override
        public String getId() {
            return "app.windowsTerminalPreview";
        }
    };

    ExternalTerminalType WINDOWS_TERMINAL = new SimplePathType("app.windowsTerminal", "wt.exe") {

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) throws Exception {
            // A weird behavior in Windows Terminal causes the trailing
            // backslash of a filepath to escape the closing quote in the title argument
            // So just remove that slash
            var fixedName = FileNames.removeTrailingSlash(configuration.getColoredTitle());
            var toExec = !ShellDialects.isPowershell(LocalShell.getShell())
                    ? CommandBuilder.of().addFile(configuration.getScriptFile())
                    : CommandBuilder.of()
                            .add("powershell", "-ExecutionPolicy", "Bypass", "-File")
                            .addQuoted(configuration.getScriptFile());
            return CommandBuilder.of()
                    .add("-w", "1", "nt", "--title")
                    .addQuoted(fixedName)
                    .add(toExec);
        }
    };

    ExternalTerminalType ALACRITTY_WINDOWS = new SimplePathType("app.alacritty", "alacritty") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            var b = CommandBuilder.of();
            if (configuration.getColor() != null) {
                b.add("-o")
                        .addQuoted("colors.primary.background='%s'"
                                .formatted(configuration.getColor().toHexString()));
            }
            return b.add("-t")
                    .addQuoted(configuration.getCleanTitle())
                    .add("-e")
                    .add("cmd")
                    .add("/c")
                    .addQuoted(configuration.getScriptFile().replaceAll(" ", "^$0"));
        }
    };
    ExternalTerminalType TABBY_WINDOWS = new WindowsType("app.tabby", "Tabby") {

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        protected void execute(Path file, LaunchConfiguration configuration) throws Exception {
            // Tabby has a very weird handling of output, even detaching with start does not prevent it from printing
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .addFile(file.toString())
                            .add("run")
                            .addFile(configuration.getScriptFile())
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
    };
    ExternalTerminalType WEZ_WINDOWS = new WindowsType("app.wezterm", "wezterm-gui") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        protected void execute(Path file, LaunchConfiguration configuration) throws Exception {
            ApplicationHelper.executeLocalApplication(
                    CommandBuilder.of().addFile(file.toString()).add("start").addFile(configuration.getScriptFile()),
                    true);
        }

        @Override
        protected Optional<Path> determineInstallation() {
            Optional<String> launcherDir;
            launcherDir = WindowsRegistry.readString(
                            WindowsRegistry.HKEY_LOCAL_MACHINE,
                            "Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\{BCF6F0DA-5B9A-408D-8562-F680AE6E1EAF}_is1",
                            "InstallLocation")
                    .map(p -> p + "\\wezterm-gui.exe");
            return launcherDir.map(Path::of);
        }
    };
    ExternalTerminalType WEZ_LINUX = new SimplePathType("app.wezterm", "wezterm-gui") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of().add("start").addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType GNOME_TERMINAL = new PathCheckType("app.gnomeTerminal", "gnome-terminal") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            try (ShellControl pc = LocalShell.getShell()) {
                ApplicationHelper.checkIsInPath(
                        pc, executable, toTranslatedString().getValue(), null);

                var toExecute = CommandBuilder.of()
                        .add(executable, "-v", "--title")
                        .addQuoted(configuration.getColoredTitle())
                        .add("--")
                        .addFile(configuration.getScriptFile())
                        .buildString(pc);
                // In order to fix this bug which also affects us:
                // https://askubuntu.com/questions/1148475/launching-gnome-terminal-from-vscode
                toExecute = "GNOME_TERMINAL_SCREEN=\"\" nohup " + toExecute + " </dev/null &>/dev/null & disown";
                pc.executeSimpleCommand(toExecute);
            }
        }
    };
    ExternalTerminalType KONSOLE = new SimplePathType("app.konsole", "konsole") {

        @Override
        public boolean supportsTabs() {
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
    ExternalTerminalType XFCE = new SimplePathType("app.xfce", "xfce4-terminal") {

        @Override
        public boolean supportsTabs() {
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
    ExternalTerminalType ELEMENTARY = new SimplePathType("app.elementaryTerminal", "io.elementary.terminal") {

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of().add("--new-tab").add("-e").addFile(configuration.getColoredTitle());
        }
    };
    ExternalTerminalType TILIX = new SimplePathType("app.tilix", "tilix") {

        @Override
        public boolean supportsTabs() {
            return false;
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
    ExternalTerminalType TERMINATOR = new SimplePathType("app.terminator", "terminator") {

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-e")
                    .addQuoted(configuration.getScriptFile())
                    .add("-T")
                    .addQuoted(configuration.getColoredTitle())
                    .add("--new-tab");
        }
    };
    ExternalTerminalType KITTY_LINUX = new SimplePathType("app.kitty", "kitty") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-1")
                    .add("-T")
                    .addQuoted(configuration.getColoredTitle())
                    .addQuoted(configuration.getScriptFile());
        }
    };
    ExternalTerminalType TERMINOLOGY = new SimplePathType("app.terminology", "terminology") {

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-T")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-2")
                    .add("-e")
                    .addQuoted(configuration.getScriptFile());
        }
    };
    ExternalTerminalType COOL_RETRO_TERM = new SimplePathType("app.coolRetroTerm", "cool-retro-term") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-T")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-e")
                    .addQuoted(configuration.getScriptFile());
        }
    };
    ExternalTerminalType GUAKE = new SimplePathType("app.guake", "guake") {

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-n", "~")
                    .add("-r")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-e")
                    .addQuoted(configuration.getScriptFile());
        }
    };
    ExternalTerminalType ALACRITTY_LINUX = new SimplePathType("app.alacritty", "alacritty") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-t")
                    .addQuoted(configuration.getCleanTitle())
                    .add("-e")
                    .addQuoted(configuration.getScriptFile());
        }
    };
    ExternalTerminalType TILDA = new SimplePathType("app.tilda", "tilda") {

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of().add("-c").addQuoted(configuration.getScriptFile());
        }
    };
    ExternalTerminalType XTERM = new SimplePathType("app.xterm", "xterm") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-title")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-e")
                    .addQuoted(configuration.getScriptFile());
        }
    };
    ExternalTerminalType DEEPIN_TERMINAL = new SimplePathType("app.deepinTerminal", "deepin-terminal") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of().add("-C").addQuoted(configuration.getScriptFile());
        }
    };
    ExternalTerminalType Q_TERMINAL = new SimplePathType("app.qTerminal", "qterminal") {

        @Override
        public boolean supportsTabs() {
            return false;
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
        public void launch(LaunchConfiguration configuration) throws Exception {
            try (ShellControl pc = LocalShell.getShell()) {
                var suffix = "\"" + configuration.getScriptFile().replaceAll("\"", "\\\\\"") + "\"";
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
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var app = this.getApplicationPath();
            if (app.isEmpty()) {
                throw new IllegalStateException("iTerm installation not found");
            }

            try (ShellControl pc = LocalShell.getShell()) {
                var a = app.get().toString();
                pc.osascriptCommand(String.format(
                                """
                                if application "%s" is not running then
                                    launch application "%s"
                                    delay 1
                                    tell application "%s"
                                        tell current tab of current window
                                            close
                                        end tell
                                    end tell
                                end if
                                tell application "%s"
                                    activate
                                    create window with default profile command "%s"
                                end tell
                                """,
                                a, a, a, a, configuration.getScriptFile().replaceAll("\"", "\\\\\"")))
                        .execute();
            }
        }
    };
    ExternalTerminalType WARP = new MacOsType("app.warp", "Warp") {
        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean shouldClear() {
            return false;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            if (!MacOsPermissions.waitForAccessibilityPermissions()) {
                return;
            }

            try (ShellControl pc = LocalShell.getShell()) {
                pc.osascriptCommand(String.format(
                                """
                        tell application "Warp" to activate
                        tell application "System Events" to tell process "Warp" to keystroke "t" using command down
                        delay 1
                        tell application "System Events"
                            tell process "Warp"
                                keystroke "%s"
                                delay 0.01
                                key code 36
                            end tell
                        end tell
                        """,
                                configuration.getScriptFile().replaceAll("\"", "\\\\\"")))
                        .execute();
            }
        }
    };
    ExternalTerminalType TABBY_MAC_OS = new MacOsType("app.tabby", "Tabby") {
        @Override
        public boolean supportsTabs() {
            return true;
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
    };
    ExternalTerminalType ALACRITTY_MACOS = new MacOsType("app.alacritty", "Alacritty") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("open", "-a")
                            .addQuoted("Alacritty.app")
                            .add("-n", "--args", "-t")
                            .addQuoted(configuration.getCleanTitle())
                            .add("-e")
                            .addFile(configuration.getScriptFile()));
        }
    };
    ExternalTerminalType WEZ_MACOS = new MacOsType("app.wezterm", "WezTerm") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var c = CommandBuilder.of()
                    .addFile(getApplicationPath()
                            .orElseThrow()
                            .resolve("Contents")
                            .resolve("MacOS")
                            .resolve("wezterm-gui")
                            .toString())
                    .add("start")
                    .addFile(configuration.getScriptFile())
                    .buildString(LocalShell.getShell());
            c = ApplicationHelper.createDetachCommand(LocalShell.getShell(), c);
            LocalShell.getShell().executeSimpleCommand(c);
        }
    };
    ExternalTerminalType KITTY_MACOS = new MacOsType("app.kitty", "kitty") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            if (!MacOsPermissions.waitForAccessibilityPermissions()) {
                return;
            }

            try (ShellControl pc = LocalShell.getShell()) {
                pc.osascriptCommand(String.format(
                                """
                                        if application "Kitty" is running then
                                            tell application "Kitty" to activate
                                            tell application "System Events" to tell process "Kitty" to keystroke "t" using command down
                                        else
                                            tell application "Kitty" to activate
                                        end if
                                        delay 1
                                        tell application "System Events"
                                            tell process "Kitty"
                                                keystroke "%s"
                                                delay 0.01
                                                key code 36
                                            end tell
                                        end tell
                                        """,
                                configuration.getScriptFile().replaceAll("\"", "\\\\\"")))
                        .execute();
            }
        }
    };
    ExternalTerminalType CUSTOM = new CustomType();
    List<ExternalTerminalType> WINDOWS_TERMINALS = List.of(
            TABBY_WINDOWS,
            ALACRITTY_WINDOWS,
            WEZ_WINDOWS,
            WINDOWS_TERMINAL_PREVIEW,
            WINDOWS_TERMINAL,
            CMD,
            PWSH,
            POWERSHELL);
    List<ExternalTerminalType> LINUX_TERMINALS = List.of(
            WEZ_LINUX,
            KONSOLE,
            XFCE,
            ELEMENTARY,
            GNOME_TERMINAL,
            TILIX,
            TERMINATOR,
            KITTY_LINUX,
            TERMINOLOGY,
            COOL_RETRO_TERM,
            GUAKE,
            ALACRITTY_LINUX,
            TILDA,
            XTERM,
            DEEPIN_TERMINAL,
            Q_TERMINAL);
    List<ExternalTerminalType> MACOS_TERMINALS =
            List.of(ITERM2, TABBY_MAC_OS, ALACRITTY_MACOS, KITTY_MACOS, WARP, WEZ_MACOS, MACOS_TERMINAL);

    @SuppressWarnings("TrivialFunctionalExpressionUsage")
    List<ExternalTerminalType> ALL = ((Supplier<List<ExternalTerminalType>>) () -> {
                var all = new ArrayList<ExternalTerminalType>();
                if (OsType.getLocal().equals(OsType.WINDOWS)) {
                    all.addAll(WINDOWS_TERMINALS);
                }
                if (OsType.getLocal().equals(OsType.LINUX)) {
                    all.addAll(LINUX_TERMINALS);
                }
                if (OsType.getLocal().equals(OsType.MACOS)) {
                    all.addAll(MACOS_TERMINALS);
                }
                // Prefer with tabs
                all.sort(Comparator.comparingInt(o -> (o.supportsTabs() ? -1 : 0)));
                all.add(CUSTOM);
                return all;
            })
            .get();

    static ExternalTerminalType determineDefault() {
        return ALL.stream()
                .filter(externalTerminalType -> !externalTerminalType.equals(CUSTOM))
                .filter(terminalType -> terminalType.isAvailable())
                .findFirst()
                .orElse(null);
    }

    boolean supportsTabs();

    default boolean supportsColoredTitle() {
        return true;
    }

    default boolean shouldClear() {
        return true;
    }

    default void launch(LaunchConfiguration configuration) throws Exception {}

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
                    throw new IOException("Unable to find installation of " + toTranslatedString());
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
        String scriptFile;

        ShellDialect scriptDialect;

        public CommandBuilder getDialectLaunchCommand() {
            var open = scriptDialect.getOpenScriptCommand(scriptFile);
            return open;
        }

        public CommandBuilder appendDialectLaunchCommand(CommandBuilder b) {
            var open = getDialectLaunchCommand();
            b.add(open);
            return b;
        }
    }

    class CustomType extends ExternalApplicationType implements ExternalTerminalType {

        public CustomType() {
            super("app.custom");
        }

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var custom = AppPrefs.get().customTerminalCommand().getValue();
            if (custom == null || custom.isBlank()) {
                throw ErrorEvent.expected(new IllegalStateException("No custom terminal command specified"));
            }

            var format = custom.toLowerCase(Locale.ROOT).contains("$cmd") ? custom : custom + " $CMD";
            try (var pc = LocalShell.getShell()) {
                var toExecute = ApplicationHelper.replaceFileArgument(format, "CMD", configuration.getScriptFile());
                // We can't be sure whether the command is blocking or not, so always make it not blocking
                if (pc.getOsType().equals(OsType.WINDOWS)) {
                    toExecute = "start \"" + configuration.getCleanTitle() + "\" " + toExecute;
                } else {
                    toExecute = "nohup " + toExecute + " </dev/null &>/dev/null & disown";
                }
                pc.executeSimpleCommand(toExecute);
            }
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public boolean isSelectable() {
            return true;
        }
    }

    abstract class MacOsType extends ExternalApplicationType.MacApplication implements ExternalTerminalType {

        public MacOsType(String id, String applicationName) {
            super(id, applicationName);
        }
    }

    @Getter
    abstract class PathCheckType extends ExternalApplicationType.PathApplication implements ExternalTerminalType {

        public PathCheckType(String id, String executable) {
            super(id, executable);
        }

        @Override
        public boolean isSelectable() {
            return true;
        }
    }

    @Getter
    abstract class SimplePathType extends PathCheckType {

        public SimplePathType(String id, String executable) {
            super(id, executable);
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var args = toCommand(configuration).buildCommandBase(LocalShell.getShell());
            launch(configuration.getColoredTitle(), args);
        }

        protected abstract CommandBuilder toCommand(LaunchConfiguration configuration) throws Exception;
    }
}
