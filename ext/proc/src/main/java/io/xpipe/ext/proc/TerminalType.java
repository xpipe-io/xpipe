package io.xpipe.ext.proc;

import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.prefs.PrefsChoiceValue;
import io.xpipe.extension.prefs.PrefsProvider;
import io.xpipe.extension.util.ApplicationHelper;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public abstract class TerminalType implements PrefsChoiceValue {

    public static final TerminalType CMD = new SimpleType("proc.cmd", "cmd", "cmd.exe") {

        @Override
        protected String toCommand(String name, String command) {
            return "cmd.exe /C " + command;
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    };

    public static final TerminalType POWERSHELL = new SimpleType("proc.powershell", "powershell", "PowerShell") {

        @Override
        protected String toCommand(String name, String command) {
            return "powershell.exe -Command " + command;
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    };

    public static final TerminalType WINDOWS_TERMINAL =
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

    public static final TerminalType GNOME_TERMINAL =
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

    public static final TerminalType KONSOLE = new SimpleType("proc.konsole", "konsole", "Konsole") {

        @Override
        protected String toCommand(String name, String command) {
            return "--new-tab -e bash -c " + command;
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    public static final TerminalType XFCE = new SimpleType("proc.xfce", "xfce4-terminal", "Xfce") {

        @Override
        protected String toCommand(String name, String command) {
            return "--tab --title \"" + name + "\" --command " + command;
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    public static final TerminalType MACOS_TERMINAL = new MacType("proc.macosTerminal", "Terminal");

    public static final TerminalType ITERM2 = new MacType("proc.iterm2", "iTerm2");

    public static final TerminalType WARP = new MacType("proc.warp", "Warp");

    public static final TerminalType CUSTOM = new TerminalType("app.custom") {

        @Override
        public void launch(String name, String command) throws Exception {
            var custom =
                    PrefsProvider.get(ProcPrefs.class).customTerminalCommand().getValue();
            if (custom == null || custom.trim().isEmpty()) {
                return;
            }

            var format = custom.contains("$cmd") ? custom : custom + " $cmd";
            ShellStore.local().create().executeSimpleCommand(format.replace("$cmd", command));
        }

        @Override
        public boolean isSelectable() {
            return true;
        }

        @Override
        public boolean isAvailable() {
            return false;
        }
    };

    public static final List<TerminalType> ALL = List.of(
                    WINDOWS_TERMINAL, POWERSHELL, CMD,
                    KONSOLE, XFCE, GNOME_TERMINAL,
                    WARP, ITERM2, MACOS_TERMINAL,
                    CUSTOM)
            .stream()
            .filter(terminalType -> terminalType.isSelectable())
            .toList();

    public static TerminalType getDefault() {
        return ALL.stream()
                .filter(terminalType -> terminalType.isAvailable()).findFirst().orElse(null);
    }

    private String id;

    public abstract void launch(String name, String command) throws Exception;

    public abstract boolean isSelectable();

    public abstract boolean isAvailable();

    static class MacType extends TerminalType {

        private final String terminalName;

        public MacType(String id, String terminalName) {
            super(id);
            this.terminalName = terminalName;
        }

        @Override
        public void launch(String name, String command) throws Exception {
            try (ShellProcessControl pc = ShellStore.local().create().start()) {
                var suffix = command.equals(pc.getShellType().getNormalOpenCommand()) ? "\"\"" : "\"" + command + "\"";
                var cmd = "osascript -e 'tell app \"" + terminalName + "\" to do script " + suffix + "'";
                pc.executeSimpleCommand(cmd);
            }
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.MAC);
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }

    public abstract static class SimpleType extends TerminalType {

        private final String executable;
        private final String displayName;

        public SimpleType(String id, String executable, String displayName) {
            super(id);
            this.executable = executable;
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
