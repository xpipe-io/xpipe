package io.xpipe.ext.proc;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.prefs.PrefsChoiceValue;
import io.xpipe.extension.prefs.PrefsProvider;
import io.xpipe.extension.util.ApplicationHelper;

import java.util.List;

public interface ExternalTerminalType extends PrefsChoiceValue {

    public static final ExternalTerminalType CMD = new SimpleType("proc.cmd", "cmd", "cmd.exe") {

        @Override
        protected String toCommand(String name, String command) {
            return "cmd.exe /C " + command;
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    };

    public static final ExternalTerminalType POWERSHELL = new SimpleType("proc.powershell", "powershell", "PowerShell") {

        @Override
        protected String toCommand(String name, String command) {
            return "powershell.exe -Command " + command;
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    };

    public static final ExternalTerminalType WINDOWS_TERMINAL =
            new SimpleType("proc.windowsTerminal", "wt.exe", "Windows Terminal") {

                @Override
                protected String toCommand(String name, String command) {
                    return "-w 1 nt --title \"" + name + "\" " + command;
                }

                @Override
                public boolean isSelectable() {
                    return OsType.getLocal().equals(OsType.WINDOWS);
                }
            };

    public static final ExternalTerminalType GNOME_TERMINAL =
            new SimpleType("proc.gnomeTerminal", "gnome-terminal", "Gnome Terminal") {

                @Override
                protected String toCommand(String name, String command) {
                    return "--title \"" + name + "\" -- " + command;
                }

                @Override
                public boolean isSelectable() {
                    return OsType.getLocal().equals(OsType.LINUX);
                }
            };

    public static final ExternalTerminalType KONSOLE = new SimpleType("proc.konsole", "konsole", "Konsole") {

        @Override
        protected String toCommand(String name, String command) {
            return "--new-tab -e bash -c " + command;
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    public static final ExternalTerminalType XFCE = new SimpleType("proc.xfce", "xfce4-terminal", "Xfce") {

        @Override
        protected String toCommand(String name, String command) {
            return "--tab --title \"" + name + "\" --command " + command;
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    public static final ExternalTerminalType MACOS_TERMINAL = new MacOsType();

    public static final ExternalTerminalType ITERM2 = new ITerm2Type();

    public static final ExternalTerminalType WARP = new WarpType();

    public static final ExternalTerminalType CUSTOM = new CustomType();

    public static final List<ExternalTerminalType> ALL = List.of(
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
            .stream()
            .filter(terminalType -> terminalType.isSelectable())
            .toList();

    public static ExternalTerminalType getDefault() {
        return ALL.stream()
                .filter(terminalType -> terminalType.isAvailable())
                .findFirst()
                .orElse(null);
    }

    public abstract void launch(String name, String command) throws Exception;

    static class MacOsType extends ExternalApplicationType.MacApplication implements ExternalTerminalType {

        public MacOsType() {
            super("proc.macosTerminal", "Terminal");
        }

        @Override
        public void launch(String name, String command) throws Exception {
            try (ShellProcessControl pc = ShellStore.local().create().start()) {
                var suffix = command.equals(pc.getShellType().getNormalOpenCommand()) ? "\"\"" : "\"" + command + "\"";
                var cmd = "osascript -e 'tell app \"" + "Terminal" + "\" to do script " + suffix + "'";
                pc.executeSimpleCommand(cmd);
            }
        }
    }

    static class CustomType extends ExternalApplicationType implements ExternalTerminalType {

        public CustomType() {
            super("proc.custom");
        }

        @Override
        public void launch(String name, String command) throws Exception {
            var custom =
                    PrefsProvider.get(ProcPrefs.class).customTerminalCommand().getValue();
            if (custom == null || custom.trim().isEmpty()) {
                return;
            }

            var format = custom.contains("$cmd") ? custom : custom + " $cmd";
            try (var pc = ShellStore.local().create().start()) {
                var toExecute = format.replace("$cmd", command);
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
            super("proc.iterm2", "iTerm2");
        }

        @Override
        public void launch(String name, String command) throws Exception {
            try (ShellProcessControl pc = ShellStore.local().create().start()) {
                var cmd = String.format(
                        """
                                        osascript - "$@" <<EOF
                                        tell application "iTerm"
                                            activate
                                            set new_term to (create window with profile "Default" command "%s")
                                        end tell
                                        EOF""",
                        command.replaceAll("\"", "\\\\\""));
                pc.executeSimpleCommand(cmd);
            }
        }
    }

    static class WarpType extends ExternalApplicationType.MacApplication implements ExternalTerminalType {

        public WarpType() {
            super("proc.warp", "Warp");
        }

        @Override
        public void launch(String name, String command) throws Exception {
            try (ShellProcessControl pc = ShellStore.local().create().start()) {
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
                        command.replaceAll("\"", "\\\\\""));
                pc.executeSimpleCommand(cmd);
            }
        }
    }

    public abstract static class SimpleType extends ExternalApplicationType.PathApplication implements ExternalTerminalType {

        private final String displayName;

        public SimpleType(String id, String executable, String displayName) {
            super(id, executable);
            this.displayName = displayName;
        }

        @Override
        public void launch(String name, String command) throws Exception {
            try (ShellProcessControl pc = ShellStore.local().create().start()) {
                ApplicationHelper.checkSupport(pc, executable, displayName);

                var toExecute = executable + " " + toCommand(name, command);
                if (pc.getOsType().equals(OsType.WINDOWS)) {
                    toExecute = "start \"" + name + "\" " + toExecute;
                } else {
                    toExecute = "nohup " + toExecute + " </dev/null &>/dev/null & disown";
                }
                pc.executeSimpleCommand(toExecute);
            }
        }

        protected abstract String toCommand(String name, String command);

        public boolean isAvailable() {
            try (ShellProcessControl pc = ShellStore.local().create().start()) {
                return pc.executeBooleanSimpleCommand(pc.getShellType().getWhichCommand(executable));
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
