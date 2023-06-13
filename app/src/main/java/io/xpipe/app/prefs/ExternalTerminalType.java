package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.ApplicationHelper;
import io.xpipe.app.util.MacOsPermissions;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface ExternalTerminalType extends PrefsChoiceValue {

    ExternalTerminalType CMD = new SimpleType("app.cmd", "cmd.exe", "cmd.exe") {

        @Override
        protected String toCommand(String name, String file) {
            return "/C \"" + file + "\"";
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    };

    ExternalTerminalType POWERSHELL_WINDOWS = new SimpleType("app.powershell", "powershell", "PowerShell") {

        @Override
        protected String toCommand(String name, String file) {
            return "-ExecutionPolicy Bypass -NoProfile -Command cmd /C '" + file + "'";
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    };

    ExternalTerminalType PWSH_WINDOWS = new SimpleType("app.pwsh", "pwsh", "PowerShell Core") {

        @Override
        protected String toCommand(String name, String file) {
            // Fix for https://github.com/PowerShell/PowerShell/issues/18530#issuecomment-1325691850
            var script = ScriptHelper.createLocalExecScript("set \"PSModulePath=\"\r\n\"" + file + "\"\npause");
            return "-ExecutionPolicy Bypass -NoProfile -Command cmd /C '" + script + "'";
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    };

    ExternalTerminalType WINDOWS_TERMINAL = new SimpleType("app.windowsTerminal", "wt.exe", "Windows Terminal") {

        @Override
        protected String toCommand(String name, String file) {
            // A weird behavior in Windows Terminal causes the trailing
            // backslash of a filepath to escape the closing quote in the title argument
            // So just remove that slash
            var fixedName = FileNames.removeTrailingSlash(name);
            return "-w 1 nt --title \"" + fixedName + "\" \"" + file + "\"";
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    };

    public abstract static class WindowsFullPathType extends ExternalApplicationType.WindowsFullPathType
            implements ExternalTerminalType {

        public WindowsFullPathType(String id) {
            super(id);
        }

        @Override
        public void launch(String name, String file, boolean elevated) throws Exception {
            var path = determinePath();
            if (path.isEmpty()) {
                throw new IOException("Unable to find installation of " + toTranslatedString());
            }

            ApplicationHelper.executeLocalApplication(
                    sc -> createCommand(sc, name, path.get().toString(), file), false);
        }

        protected abstract String createCommand(ShellControl shellControl, String name, String path, String file);
    }

    ExternalTerminalType TABBY_WINDOWS = new WindowsFullPathType("app.tabbyWindows") {

        @Override
        protected String createCommand(ShellControl shellControl, String name, String path, String file) {
            return shellControl.getShellDialect().fileArgument(path) + " run "
                    + shellControl.getShellDialect().fileArgument(file);
        }

        @Override
        protected Optional<Path> determinePath() {
            Optional<String> launcherDir;
            launcherDir = WindowsRegistry.readString(
                            WindowsRegistry.HKEY_CURRENT_USER,
                            "SOFTWARE\\71445fac-d6ef-5436-9da7-5a323762d7f5",
                            "InstallLocation")
                    .map(p -> p + "\\Tabby.exe");
            return launcherDir.map(Path::of);
        }
    };

    ExternalTerminalType GNOME_TERMINAL = new SimpleType("app.gnomeTerminal", "gnome-terminal", "Gnome Terminal") {

        @Override
        public void launch(String name, String file, boolean elevated) throws Exception {
            try (ShellControl pc = LocalStore.getShell()) {
                ApplicationHelper.checkSupport(pc, executable, getDisplayName(), null);

                var toExecute = executable + " " + toCommand(name, file);
                // In order to fix this bug which also affects us:
                // https://askubuntu.com/questions/1148475/launching-gnome-terminal-from-vscode
                toExecute = "GNOME_TERMINAL_SCREEN=\"\" nohup " + toExecute + " </dev/null &>/dev/null & disown";
                pc.executeSimpleCommand(toExecute);
            }
        }

        @Override
        protected String toCommand(String name, String file) {
            return "-v --title \"" + name + "\" -- \"" + file + "\"";
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    ExternalTerminalType KONSOLE = new SimpleType("app.konsole", "konsole", "Konsole") {

        @Override
        protected String toCommand(String name, String file) {
            // Note for later: When debugging konsole launches, it will always open as a child process of
            // IntelliJ/XPipe even though we try to detach it.
            // This is not the case for production where it works as expected
            return "--new-tab -e \"" + file + "\"";
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    ExternalTerminalType XFCE = new SimpleType("app.xfce", "xfce4-terminal", "Xfce") {

        @Override
        protected String toCommand(String name, String file) {
            return "--tab --title \"" + name + "\" --command \"" + file + "\"";
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    ExternalTerminalType MACOS_TERMINAL = new MacOsTerminalType();

    ExternalTerminalType ITERM2 = new ITerm2Type();

    ExternalTerminalType WARP = new WarpType();

    ExternalTerminalType TABBY_MAC_OS = new TabbyMacOsType();

    ExternalTerminalType CUSTOM = new CustomType();

    List<ExternalTerminalType> ALL = Stream.of(
                    TABBY_WINDOWS,
                    WINDOWS_TERMINAL,
                    PWSH_WINDOWS,
                    POWERSHELL_WINDOWS,
                    CMD,
                    KONSOLE,
                    XFCE,
                    GNOME_TERMINAL,
                    ITERM2,
                    TABBY_MAC_OS,
                    WARP,
                    MACOS_TERMINAL,
                    CUSTOM)
            .filter(terminalType -> terminalType.isSelectable())
            .toList();

    static ExternalTerminalType getDefault() {
        return ALL.stream()
                .filter(externalTerminalType -> !externalTerminalType.equals(CUSTOM))
                .filter(terminalType -> terminalType.isAvailable())
                .findFirst()
                .orElse(null);
    }

    void launch(String name, String file, boolean elevated) throws Exception;

    class MacOsTerminalType extends ExternalApplicationType.MacApplication implements ExternalTerminalType {

        public MacOsTerminalType() {
            super("app.macosTerminal", "Terminal");
        }

        @Override
        public void launch(String name, String file, boolean elevated) throws Exception {
            try (ShellControl pc = LocalStore.getShell()) {
                var suffix = file.equals(pc.getShellDialect().getOpenCommand())
                        ? "\"\""
                        : "\"" + file.replaceAll("\"", "\\\\\"") + "\"";
                pc.osascriptCommand(String.format(
                                """
                                activate application "Terminal"
                                tell app "Terminal" to do script %s
                                """,
                                suffix))
                        .execute();
            }
        }
    }

    class CustomType extends ExternalApplicationType implements ExternalTerminalType {

        public CustomType() {
            super("app.custom");
        }

        @Override
        public void launch(String name, String file, boolean elevated) throws Exception {
            var custom = AppPrefs.get().customTerminalCommand().getValue();
            if (custom == null || custom.isBlank()) {
                throw new IllegalStateException("No custom terminal command specified");
            }

            var format = custom.contains("$cmd") ? custom : custom + " $cmd";
            try (var pc = LocalStore.getShell()) {
                var toExecute = format.replace("$cmd", "\"" + file + "\"");
                if (pc.getOsType().equals(OsType.WINDOWS)) {
                    toExecute = "start \"" + name + "\" " + toExecute;
                } else {
                    toExecute = "nohup " + toExecute + " </dev/null &>/dev/null & disown";
                }
                pc.executeSimpleCommand(toExecute);
            }
        }

        @Override
        public boolean isSelectable() {
            return true;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }

    class ITerm2Type extends ExternalApplicationType.MacApplication implements ExternalTerminalType {

        public ITerm2Type() {
            super("app.iterm2", "iTerm");
        }

        @Override
        public void launch(String name, String file, boolean elevated) throws Exception {
            try (ShellControl pc = LocalStore.getShell()) {
                // TODO: Wait for launch. But how?
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
                                    create window with default profile command "%s"
                                end tell
                                """,
                                file.replaceAll("\"", "\\\\\"")))
                        .execute();
            }
        }
    }

    class TabbyMacOsType extends ExternalApplicationType.MacApplication implements ExternalTerminalType {

        public TabbyMacOsType() {
            super("app.tabbyMacOs", "Tabby");
        }

        @Override
        public void launch(String name, String file, boolean elevated) throws Exception {
            try (ShellControl pc = LocalStore.getShell()) {
                pc.osascriptCommand(String.format(
                        """
                        %s/Contents/MacOS/Tabby run %s
                        """, getApplicationPath().orElseThrow(), pc.getShellDialect().fileArgument(file)))
                        .execute();
            }
        }
    }

    class WarpType extends ExternalApplicationType.MacApplication implements ExternalTerminalType {

        public WarpType() {
            super("app.warp", "Warp");
        }

        @Override
        public void launch(String name, String file, boolean elevated) throws Exception {
            if (!MacOsPermissions.waitForAccessibilityPermissions()) {
                return;
            }

            try (ShellControl pc = LocalStore.getShell()) {
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
                                file.replaceAll("\"", "\\\\\"")))
                        .execute();
            }
        }
    }

    @Getter
    abstract class SimpleType extends ExternalApplicationType.PathApplication implements ExternalTerminalType {

        private final String displayName;

        public SimpleType(String id, String executable, String displayName) {
            super(id, executable);
            this.displayName = displayName;
        }

        @Override
        public void launch(String name, String file, boolean elevated) throws Exception {
            if (elevated) {
                if (OsType.getLocal().equals(OsType.WINDOWS)) {
                    try (ShellControl pc = LocalStore.getShell()
                            .subShell(ShellDialects.POWERSHELL)
                            .start()) {
                        ApplicationHelper.checkSupport(pc, executable, displayName, null);
                        var toExecute = "Start-Process \"" + executable + "\" -Verb RunAs -ArgumentList \""
                                + toCommand(name, file).replaceAll("\"", "`\"") + "\"";
                        pc.executeSimpleCommand(toExecute);
                    }
                    return;
                }
            }

            try (ShellControl pc = LocalStore.getShell()) {
                ApplicationHelper.checkSupport(pc, executable, displayName, null);

                var toExecute = executable + " " + toCommand(name, file);
                if (pc.getOsType().equals(OsType.WINDOWS)) {
                    toExecute = "start \"" + name + "\" " + toExecute;
                } else {
                    toExecute = "nohup " + toExecute + " </dev/null &>/dev/null & disown";
                }
                pc.executeSimpleCommand(toExecute);
            }
        }

        protected abstract String toCommand(String name, String file);

        public boolean isAvailable() {
            try (ShellControl pc = LocalStore.getShell()) {
                return pc.executeSimpleBooleanCommand(pc.getShellDialect().getWhichCommand(executable));
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
                return false;
            }
        }

        @Override
        public boolean isSelectable() {
            return true;
        }
    }
}
