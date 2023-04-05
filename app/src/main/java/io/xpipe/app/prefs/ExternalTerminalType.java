package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.ApplicationHelper;
import io.xpipe.app.util.MacOsPermissions;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import lombok.Getter;

import java.util.List;
import java.util.stream.Stream;

public interface ExternalTerminalType extends PrefsChoiceValue {

    public static final ExternalTerminalType CMD = new SimpleType("cmd", "cmd", "cmd.exe") {

        @Override
        protected String toCommand(String name, String file) {
            return "cmd.exe /C \"" + file + "\"";
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    };

    public static final ExternalTerminalType POWERSHELL =
            new SimpleType("powershell", "powershell", "PowerShell") {

                @Override
                protected String toCommand(String name, String file) {
                    return "powershell.exe -ExecutionPolicy Bypass -Command \"" + file + "\"";
                }

                @Override
                public boolean isSelectable() {
                    return OsType.getLocal().equals(OsType.WINDOWS);
                }
            };

    public static final ExternalTerminalType WINDOWS_TERMINAL =
            new SimpleType("windowsTerminal", "wt.exe", "Windows Terminal") {

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

    public static final ExternalTerminalType GNOME_TERMINAL =
            new SimpleType("gnomeTerminal", "gnome-terminal", "Gnome Terminal") {

                @Override
                public void launch(String name, String file) throws Exception {
                    try (ShellControl pc = LocalStore.getShell()) {
                        ApplicationHelper.checkSupport(pc, executable, getDisplayName());

                        var toExecute = executable + " " + toCommand(name, file);
                        // In order to fix this bug which also affects us:
                        // https://askubuntu.com/questions/1148475/launching-gnome-terminal-from-vscode
                        toExecute =
                                "GNOME_TERMINAL_SCREEN=\"\" nohup " + toExecute + " </dev/null &>/dev/null & disown";
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

    public static final ExternalTerminalType KONSOLE = new SimpleType("konsole", "konsole", "Konsole") {

        @Override
        protected String toCommand(String name, String file) {
            return "--new-tab -e \"" + file + "\"";
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    public static final ExternalTerminalType XFCE = new SimpleType("xfce", "xfce4-terminal", "Xfce") {

        @Override
        protected String toCommand(String name, String file) {
            return "--tab --title \"" + name + "\" --command \"" + file + "\"";
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    public static final ExternalTerminalType MACOS_TERMINAL = new MacOsTerminalType();

    public static final ExternalTerminalType ITERM2 = new ITerm2Type();

    public static final ExternalTerminalType WARP = new WarpType();

    public static final ExternalTerminalType CUSTOM = new CustomType();

    public static final List<ExternalTerminalType> ALL = Stream.of(
                    WINDOWS_TERMINAL,
                    POWERSHELL,
                    CMD,
                    KONSOLE,
                    XFCE,
                    GNOME_TERMINAL,
                    WARP,
                    ITERM2,
                    MACOS_TERMINAL,
                    CUSTOM)
            .filter(terminalType -> terminalType.isSelectable())
            .toList();

    public static ExternalTerminalType getDefault() {
        return ALL.stream()
                .filter(externalTerminalType -> !externalTerminalType.equals(CUSTOM))
                .filter(terminalType -> terminalType.isAvailable())
                .findFirst()
                .orElse(null);
    }

    public abstract void launch(String name, String file) throws Exception;

    static class MacOsTerminalType extends ExternalApplicationType.MacApplication implements ExternalTerminalType {

        public MacOsTerminalType() {
            super("macosTerminal", "Terminal");
        }

        @Override
        public void launch(String name, String file) throws Exception {
            try (ShellControl pc = LocalStore.getShell()) {
                var suffix = file.equals(pc.getShellDialect().getOpenCommand())
                        ? "\"\""
                        : "\"" + file.replaceAll("\"", "\\\\\"") + "\"";
                var cmd = "osascript -e 'tell app \"" + "Terminal" + "\" to do script " + suffix + "'";
                pc.executeSimpleCommand(cmd);
            }
        }
    }

    static class CustomType extends ExternalApplicationType implements ExternalTerminalType {

        public CustomType() {
            super("custom");
        }

        @Override
        public void launch(String name, String file) throws Exception {
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

    static class ITerm2Type extends ExternalApplicationType.MacApplication implements ExternalTerminalType {

        public ITerm2Type() {
            super("iterm2", "iTerm2");
        }

        @Override
        public void launch(String name, String file) throws Exception {
            try (ShellControl pc = LocalStore.getShell()) {
                var cmd = String.format(
                        """
                                osascript - "$@" <<EOF
                                if application "iTerm" is running then
                                    tell application "iTerm"
                                    create window with profile "Default" command "%s"
                                    end tell
                                else
                                    activate application "iTerm"
                                    delay 1
                                    tell application "iTerm"
                                        tell current window
                                            tell current session
                                               write text "%s"
                                            end tell
                                        end tell
                                    end tell
                                end if
                                EOF""",
                        file.replaceAll("\"", "\\\\\""), file.replaceAll("\"", "\\\\\""));
                pc.executeSimpleCommand(cmd);
            }
        }
    }

    static class WarpType extends ExternalApplicationType.MacApplication implements ExternalTerminalType {

        public WarpType() {
            super("warp", "Warp");
        }

        @Override
        public void launch(String name, String file) throws Exception {
            if (!MacOsPermissions.waitForAccessibilityPermissions()) {
                return;
            }

            try (ShellControl pc = LocalStore.getShell()) {
                var cmd = String.format(
                        """
                                osascript - "$@" <<EOF
                                tell application "Warp" to activate
                                tell application "System Events" to tell process "Warp" to keystroke "t" using command down
                                delay 1
                                tell application "System Events"
                                    tell process "Warp"
                                        keystroke "%s"
                                        key code 36
                                    end tell
                                end tell
                                EOF
                                        """,
                        file.replaceAll("\"", "\\\\\""));
                pc.executeSimpleCommand(cmd);
            }
        }
    }

    @Getter
    public abstract static class SimpleType extends ExternalApplicationType.PathApplication
            implements ExternalTerminalType {

        private final String displayName;

        public SimpleType(String id, String executable, String displayName) {
            super(id, executable);
            this.displayName = displayName;
        }

        @Override
        public void launch(String name, String file) throws Exception {
            try (ShellControl pc = LocalStore.getShell()) {
                ApplicationHelper.checkSupport(pc, executable, displayName);

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
                return pc.executeBooleanSimpleCommand(pc.getShellDialect().getWhichCommand(executable));
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
