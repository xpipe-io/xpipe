package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.ApplicationHelper;
import io.xpipe.app.util.MacOsPermissions;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

public interface ExternalTerminalType extends PrefsChoiceValue {

    ExternalTerminalType CMD = new SimplePathType("app.cmd", "cmd.exe") {

        @Override
        protected CommandBuilder toCommand(String name, String file) {
            return CommandBuilder.of().add("/C").addFile(file);
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    };

    ExternalTerminalType POWERSHELL_WINDOWS = new SimplePathType("app.powershell", "powershell") {

        @Override
        protected CommandBuilder toCommand(String name, String file) {
            return CommandBuilder.of().add("-ExecutionPolicy", "Bypass", "-NoProfile", "-Command", "cmd", "/C").addFile(file);
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    };

    ExternalTerminalType PWSH_WINDOWS = new SimplePathType("app.pwsh", "pwsh") {

        @Override
        protected CommandBuilder toCommand(String name, String file) {
            // Fix for https://github.com/PowerShell/PowerShell/issues/18530#issuecomment-1325691850
            return CommandBuilder.of().add("-ExecutionPolicy", "Bypass", "-NoProfile", "-Command", "cmd", "/C").add(sc -> {
                var script = ScriptHelper.createLocalExecScript("set \"PSModulePath=\"\r\n\"" + file + "\"\npause");
                return sc.getShellDialect().fileArgument(script);
            });
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    };

    ExternalTerminalType WINDOWS_TERMINAL = new SimplePathType("app.windowsTerminal", "wt.exe") {

        @Override
        protected CommandBuilder toCommand(String name, String file) {
            // A weird behavior in Windows Terminal causes the trailing
            // backslash of a filepath to escape the closing quote in the title argument
            // So just remove that slash
            var fixedName = FileNames.removeTrailingSlash(name);
            return CommandBuilder.of().add("-w", "1", "nt", "--title").addQuoted(fixedName).addFile(file);
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    };

    ExternalTerminalType ALACRITTY_WINDOWS = new SimplePathType("app.alacrittyWindows", "alacritty") {

        @Override
        protected CommandBuilder toCommand(String name, String file) {
            return CommandBuilder.of()
                    .add("-t")
                    .addQuoted(name)
                    .add("-e")
                    .add("cmd")
                    .add("/c")
                    .addQuoted(file.replaceAll(" ", "^$0"));
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    };
    abstract class WindowsType extends ExternalApplicationType.WindowsType
            implements ExternalTerminalType {

        public WindowsType(String id, String executable) {
            super(id, executable);
        }

        @Override
        public void launch(String name, String file) throws Exception {
            var location = determineFromPath();
            if (location.isEmpty()) {
                location = determineInstallation();
                if (location.isEmpty()) {
                    throw new IOException("Unable to find installation of " + toTranslatedString());
                }
            }

            Optional<Path> finalLocation = location;
            ApplicationHelper.executeLocalApplication(
                    sc -> createCommand(sc, name, finalLocation.get().toString(), file), false);
        }

        protected abstract String createCommand(ShellControl shellControl, String name, String path, String file);
    }

    ExternalTerminalType TABBY_WINDOWS = new WindowsType("app.tabbyWindows", "Tabby") {

        @Override
        protected String createCommand(ShellControl shellControl, String name, String path, String file) {
            return shellControl.getShellDialect().fileArgument(path) + " run "
                    + shellControl.getShellDialect().fileArgument(file);
        }

        @Override
        protected Optional<Path> determineInstallation() {
            Optional<String> launcherDir;
            launcherDir = WindowsRegistry.readString(
                            WindowsRegistry.HKEY_CURRENT_USER,
                            "SOFTWARE\\71445fac-d6ef-5436-9da7-5a323762d7f5",
                            "InstallLocation")
                    .map(p -> p + "\\Tabby.exe");
            return launcherDir.map(Path::of);
        }
    };

//    ExternalTerminalType HYPER_WINDOWS = new WindowsFullPathType("app.hyperWindows") {
//
//        @Override
//        protected String createCommand(ShellControl shellControl, String name, String path, String file) {
//            return shellControl.getShellDialect().fileArgument(path) + " "
//                    + shellControl.getShellDialect().fileArgument(file);
//        }
//
//        @Override
//        protected Optional<Path> determinePath() {
//            Optional<String> launcherDir;
//            launcherDir = WindowsRegistry.readString(
//                            WindowsRegistry.HKEY_CURRENT_USER,
//                            "SOFTWARE\\ac619139-e2f9-5cb9-915f-69b22e7bff50",
//                            "InstallLocation")
//                    .map(p -> p + "\\Hyper.exe");
//            return launcherDir.map(Path::of);
//        }
//    };

    ExternalTerminalType GNOME_TERMINAL = new SimplePathType("app.gnomeTerminal", "gnome-terminal") {

        @Override
        public void launch(String name, String file) throws Exception {
            try (ShellControl pc = LocalStore.getShell()) {
                ApplicationHelper.checkSupport(pc, executable, toTranslatedString(), null);

                var toExecute = executable + " " + toCommand(name, file).build(pc);
                // In order to fix this bug which also affects us:
                // https://askubuntu.com/questions/1148475/launching-gnome-terminal-from-vscode
                toExecute = "GNOME_TERMINAL_SCREEN=\"\" nohup " + toExecute + " </dev/null &>/dev/null & disown";
                pc.executeSimpleCommand(toExecute);
            }
        }

        @Override
        protected CommandBuilder toCommand(String name, String file) {
            return CommandBuilder.of().add("-v", "--title").addQuoted(name).add("--").addFile(file);
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    ExternalTerminalType KONSOLE = new SimplePathType("app.konsole", "konsole") {

        @Override
        protected CommandBuilder toCommand(String name, String file) {
            // Note for later: When debugging konsole launches, it will always open as a child process of
            // IntelliJ/XPipe even though we try to detach it.
            // This is not the case for production where it works as expected
            return CommandBuilder.of().add("--new-tab", "-e").addFile(file);
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    ExternalTerminalType XFCE = new SimplePathType("app.xfce", "xfce4-terminal") {

        @Override
        protected CommandBuilder toCommand(String name, String file) {
            return CommandBuilder.of().add("--tab", "--title").addQuoted(name).add("--command").addFile(file);
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    ExternalTerminalType TERMINATOR = new SimplePathType("app.terminator", "terminator") {

        @Override
        protected CommandBuilder toCommand(String name, String file) {
            return CommandBuilder.of()
                    .add("-e")
                    .addQuoted(file)
                    .add("-T")
                    .addQuoted(name)
                    .add("--new-tab");
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    ExternalTerminalType KITTY = new SimplePathType("app.kitty", "kitty") {

        @Override
        protected CommandBuilder toCommand(String name, String file) {
            return CommandBuilder.of().add("-T").addQuoted(name).addQuoted(file);
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    ExternalTerminalType TERMINOLOGY = new SimplePathType("app.terminology", "terminology") {

        @Override
        protected CommandBuilder toCommand(String name, String file) {
            return CommandBuilder.of()
                    .add("-T")
                    .addQuoted(name)
                    .add("-2")
                    .add("-e")
                    .addQuoted(file);
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    ExternalTerminalType COOL_RETRO_TERM = new SimplePathType("app.coolRetroTerm", "cool-retro-term") {

        @Override
        protected CommandBuilder toCommand(String name, String file) {
            return CommandBuilder.of()
                    .add("-T")
                    .addQuoted(name)
                    .add("-e")
                    .addQuoted(file);
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    ExternalTerminalType GUAKE = new SimplePathType("app.guake", "guake") {

        @Override
        protected CommandBuilder toCommand(String name, String file) {
            return CommandBuilder.of()
                    .add("-r")
                    .addQuoted(name)
                    .add("-e")
                    .addQuoted(file);
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    ExternalTerminalType ALACRITTY = new SimplePathType("app.alacritty", "alacritty") {

        @Override
        protected CommandBuilder toCommand(String name, String file) {
            return CommandBuilder.of()
                    .add("-t")
                    .addQuoted(name)
                    .add("-e")
                    .addQuoted(file);
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    };

    ExternalTerminalType TILDA = new SimplePathType("app.tilda", "tilda") {

        @Override
        protected CommandBuilder toCommand(String name, String file) {
            return CommandBuilder.of().add("-c").addQuoted(file);
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

    ExternalTerminalType ALACRITTY_MACOS = new MacOsType("app.alacrittyMacOs", "Alacritty") {

        @Override
        public void launch(String name, String file) throws Exception {
            try (ShellControl pc = new LocalStore().control().start()) {
                pc.command(String.format(
                                """
                                %s/Contents/MacOS/alacritty -t "%s" -e %s
                                """,
                                getApplicationPath().orElseThrow(),
                                name,
                                pc.getShellDialect().fileArgument(file)))
                        .execute();
            }
        }
    };

    ExternalTerminalType KITTY_MACOS = new MacOsType("app.kittyMacOs", "kitty") {

        @Override
        public void launch(String name, String file) throws Exception {
            try (ShellControl pc = new LocalStore().control().start()) {
                pc.command(String.format(
                                """
                                %s/Contents/MacOS/kitty -T "%s" %s
                                """,
                                getApplicationPath().orElseThrow(),
                                name,
                                pc.getShellDialect().fileArgument(file)))
                        .execute();
            }
        }
    };

    ExternalTerminalType CUSTOM = new CustomType();

    List<ExternalTerminalType> ALL = Stream.of(
                    TABBY_WINDOWS,
                    ALACRITTY_WINDOWS,
                    WINDOWS_TERMINAL,
                    PWSH_WINDOWS,
                    POWERSHELL_WINDOWS,
                    CMD,
                    KONSOLE,
                    XFCE,
                    GNOME_TERMINAL,
                    TERMINATOR,
                    KITTY,
                    TERMINOLOGY,
                    COOL_RETRO_TERM,
                    GUAKE,
                    ALACRITTY,
                    TILDA,
                    ITERM2,
                    TABBY_MAC_OS,
                    ALACRITTY_MACOS,
                    KITTY_MACOS,
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

    void launch(String name, String file) throws Exception;

    class MacOsTerminalType extends ExternalApplicationType.MacApplication implements ExternalTerminalType {

        public MacOsTerminalType() {
            super("app.macosTerminal", "Terminal");
        }

        @Override
        public void launch(String name, String file) throws Exception {
            try (ShellControl pc = LocalStore.getShell()) {
                var suffix = "\"" + file.replaceAll("\"", "\\\\\"") + "\"";
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
        public void launch(String name, String file) throws Exception {
            var custom = AppPrefs.get().customTerminalCommand().getValue();
            if (custom == null || custom.isBlank()) {
                throw new IllegalStateException("No custom terminal command specified");
            }

            var format = custom.toLowerCase(Locale.ROOT).contains("$cmd") ? custom : custom + " $CMD";
            try (var pc = LocalStore.getShell()) {
                var toExecute = ApplicationHelper.replaceFileArgument(format, "CMD", file);
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
        public void launch(String name, String file) throws Exception {
            var app = this.getApplicationPath();
            if (app.isEmpty()) {
                throw new IllegalStateException("iTerm installation not found");
            }

            try (ShellControl pc = LocalStore.getShell()) {
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
                                    create window with default profile command "%s"
                                end tell
                                """,
                                a, a, a, a, file.replaceAll("\"", "\\\\\"")))
                        .execute();
            }
        }
    }

    class TabbyMacOsType extends ExternalApplicationType.MacApplication implements ExternalTerminalType {

        public TabbyMacOsType() {
            super("app.tabbyMacOs", "Tabby");
        }

        @Override
        public void launch(String name, String file) throws Exception {
            try (ShellControl pc = new LocalStore().control().start()) {
                pc.command(String.format(
                        """
                        %s/Contents/MacOS/Tabby run %s
                        """,
                                getApplicationPath().orElseThrow(),
                                pc.getShellDialect().fileArgument(file)))
                        .execute();
            }
        }
    }

    class WarpType extends ExternalApplicationType.MacApplication implements ExternalTerminalType {

        public WarpType() {
            super("app.warp", "Warp");
        }

        @Override
        public void launch(String name, String file) throws Exception {
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

    abstract class MacOsType extends ExternalApplicationType.MacApplication implements ExternalTerminalType {

        public MacOsType(String id, String applicationName) {
            super(id, applicationName);
        }
    }

    @Getter
    abstract class SimplePathType extends ExternalApplicationType.PathApplication implements ExternalTerminalType {

        public SimplePathType(String id, String executable) {
            super(id, executable);
        }

        @Override
        public void launch(String name, String file) throws Exception {
            try (ShellControl pc = LocalStore.getShell()) {
                ApplicationHelper.checkSupport(pc, executable, toTranslatedString(), null);

                var toExecute = executable + " " + toCommand(name, file).build(pc);
                if (pc.getOsType().equals(OsType.WINDOWS)) {
                    toExecute = "start \"" + name + "\" " + toExecute;
                } else {
                    toExecute = "nohup " + toExecute + " </dev/null &>/dev/null & disown";
                }
                pc.executeSimpleCommand(toExecute);
            }
        }

        protected abstract CommandBuilder toCommand(String name, String file);

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
