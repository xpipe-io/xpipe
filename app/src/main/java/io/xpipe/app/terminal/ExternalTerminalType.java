package io.xpipe.app.terminal;

import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.storage.DataColor;
import io.xpipe.app.util.*;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.FailableFunction;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import lombok.Getter;
import lombok.Value;
import lombok.With;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public interface ExternalTerminalType extends PrefsChoiceValue {

    //    ExternalTerminalType PUTTY = new WindowsType("app.putty","putty") {
    //
    //        @Override
    //        protected Optional<Path> determineInstallation() {
    //            try {
    //                var r = WindowsRegistry.local().readValue(WindowsRegistry.HKEY_LOCAL_MACHINE,
    //                        "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\Xshell.exe");
    //                return r.map(Path::of);
    //            }  catch (Exception e) {
    //                ErrorEvent.fromThrowable(e).omit().handle();
    //                return Optional.empty();
    //            }
    //        }
    //
    //        @Override
    //        public boolean supportsTabs() {
    //            return true;
    //        }
    //
    //        @Override
    //        public boolean isRecommended() {
    //            return false;
    //        }
    //
    //        @Override
    //        public boolean supportsColoredTitle() {
    //            return false;
    //        }
    //
    //        @Override
    //        protected void execute(Path file, LaunchConfiguration configuration) throws Exception {
    //            try (var sc = LocalShell.getShell()) {
    //                SshLocalBridge.init();
    //                var b = SshLocalBridge.get();
    //                var command = CommandBuilder.of().addFile(file.toString()).add("-ssh", "localhost",
    // "-l").addQuoted(b.getUser())
    //                        .add("-i").addFile(b.getIdentityKey().toString()).add("-P", "" +
    // b.getPort()).add("-hostkey").addFile(b.getPubHostKey().toString());
    //                sc.executeSimpleCommand(command);
    //            }
    //        }
    //    };

    static ExternalTerminalType determineNonSshBridgeFallback(ExternalTerminalType type) {
        if (type == XSHELL || type == MOBAXTERM || type == SECURECRT) {
            return ProcessControlProvider.get().getEffectiveLocalDialect() == ShellDialects.CMD ? CMD : POWERSHELL;
        }

        if (type != TERMIUS) {
            return type;
        }

        switch (OsType.getLocal()) {
            case OsType.Linux linux -> {
                // This should not be termius as all others take precedence
                var def = determineDefault(null);
                // If there's no other terminal available, use a fallback which won't work
                return def != TERMIUS ? def : XTERM;
            }
            case OsType.MacOs macOs -> {
                return MACOS_TERMINAL;
            }
            case OsType.Windows windows -> {
                return ProcessControlProvider.get().getEffectiveLocalDialect() == ShellDialects.CMD ? CMD : POWERSHELL;
            }
        }
    }

    ExternalTerminalType XSHELL = new WindowsType("app.xShell", "Xshell") {

        @Override
        protected Optional<Path> determineInstallation() {
            try {
                var r = WindowsRegistry.local()
                        .readValue(
                                WindowsRegistry.HKEY_LOCAL_MACHINE,
                                "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\Xshell.exe");
                return r.map(Path::of);
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
                return Optional.empty();
            }
        }

        @Override
        public String getWebsite() {
            return "https://www.netsarang.com/en/xshell/";
        }

        @Override
        public boolean supportsTabs() {
            return true;
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
        protected void execute(Path file, LaunchConfiguration configuration) throws Exception {
            SshLocalBridge.init();
            if (!showInfo()) {
                return;
            }

            try (var sc = LocalShell.getShell()) {
                var b = SshLocalBridge.get();
                var keyName = b.getIdentityKey().getFileName().toString();
                var command = CommandBuilder.of()
                        .addFile(file.toString())
                        .add("-url")
                        .addQuoted("ssh://" + b.getUser() + "@localhost:" + b.getPort())
                        .add("-i", keyName);
                sc.executeSimpleCommand(command);
            }
        }

        private boolean showInfo() {
            boolean set = AppCache.getBoolean("xshellSetup", false);
            if (set) {
                return true;
            }

            var b = SshLocalBridge.get();
            var keyName = b.getIdentityKey().getFileName().toString();
            var r = AppWindowHelper.showBlockingAlert(alert -> {
                alert.setTitle(AppI18n.get("xshellSetup"));
                alert.setAlertType(Alert.AlertType.NONE);

                var activated = AppI18n.get()
                        .getMarkdownDocumentation("app:xshellSetup")
                        .formatted(b.getIdentityKey(), keyName);
                var markdown = new MarkdownComp(activated, s -> s)
                        .prefWidth(450)
                        .prefHeight(400)
                        .createRegion();
                alert.getDialogPane().setContent(markdown);

                alert.getButtonTypes().add(new ButtonType(AppI18n.get("ok"), ButtonBar.ButtonData.OK_DONE));
            });
            r.filter(buttonType -> buttonType.getButtonData().isDefaultButton());
            r.ifPresent(buttonType -> {
                AppCache.update("xshellSetup", true);
            });
            return r.isPresent();
        }
    };

    ExternalTerminalType SECURECRT = new WindowsType("app.secureCrt", "SecureCRT") {

        @Override
        protected Optional<Path> determineInstallation() {
            try (var sc = LocalShell.getShell().start()) {
                var env = sc.executeSimpleStringCommand(
                        sc.getShellDialect().getPrintEnvironmentVariableCommand("ProgramFiles"));
                var file = Path.of(env, "VanDyke Software\\SecureCRT\\SecureCRT.exe");
                if (!Files.exists(file)) {
                    return Optional.empty();
                }

                return Optional.of(file);
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
                return Optional.empty();
            }
        }

        @Override
        public boolean supportsTabs() {
            return true;
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
        public String getWebsite() {
            return "https://www.vandyke.com/products/securecrt/";
        }

        @Override
        protected void execute(Path file, LaunchConfiguration configuration) throws Exception {
            try (var sc = LocalShell.getShell()) {
                SshLocalBridge.init();
                var b = SshLocalBridge.get();
                var command = CommandBuilder.of()
                        .addFile(file.toString())
                        .add("/T")
                        .add("/SSH2", "/ACCEPTHOSTKEYS", "/I")
                        .addFile(b.getIdentityKey().toString())
                        .add("/P", "" + b.getPort())
                        .add("/L")
                        .addQuoted(b.getUser())
                        .add("localhost");
                sc.executeSimpleCommand(command);
            }
        }
    };

    ExternalTerminalType MOBAXTERM = new WindowsType("app.mobaXterm", "MobaXterm") {

        @Override
        protected Optional<Path> determineInstallation() {
            try {
                var r = WindowsRegistry.local()
                        .readValue(WindowsRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Classes\\mobaxterm\\DefaultIcon");
                return r.map(Path::of);
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
                return Optional.empty();
            }
        }

        @Override
        public boolean supportsTabs() {
            return true;
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
        public String getWebsite() {
            return "https://mobaxterm.mobatek.net/";
        }

        @Override
        protected void execute(Path file, LaunchConfiguration configuration) throws Exception {
            try (var sc = LocalShell.getShell()) {
                SshLocalBridge.init();
                var b = SshLocalBridge.get();
                var command = CommandBuilder.of()
                        .addFile("ssh")
                        .addQuoted(b.getUser() + "@localhost")
                        .add("-i")
                        .add("\"$(cygpath \"" + b.getIdentityKey().toString() + "\")\"")
                        .add("-p")
                        .add("" + b.getPort());
                // Don't use local shell to build as it uses cygwin
                var rawCommand = command.buildSimple();
                var script = ScriptHelper.getExecScriptFile(sc, "sh");
                Files.writeString(Path.of(script.toString()), rawCommand);
                var fixedFile = script.toString().replaceAll("\\\\", "/").replaceAll("\\s", "\\$0");
                sc.command(CommandBuilder.of()
                                .addFile(file.toString())
                                .add("-newtab")
                                .add(fixedFile))
                        .execute();
            }
        }
    };

    ExternalTerminalType TERMIUS = new ExternalTerminalType() {

        @Override
        public String getId() {
            return "app.termius";
        }

        @Override
        public boolean isAvailable() {
            try (var sc = LocalShell.getShell()) {
                return switch (OsType.getLocal()) {
                    case OsType.Linux linux -> {
                        yield Files.exists(Path.of("/opt/Termius"));
                    }
                    case OsType.MacOs macOs -> {
                        yield Files.exists(Path.of("/Applications/Termius.app"));
                    }
                    case OsType.Windows windows -> {
                        var r = WindowsRegistry.local()
                                .readValue(WindowsRegistry.HKEY_CURRENT_USER, "SOFTWARE\\Classes\\termius");
                        yield r.isPresent();
                    }
                };
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
                return false;
            }
        }

        @Override
        public String getWebsite() {
            return "https://termius.com/";
        }

        @Override
        public boolean supportsTabs() {
            return true;
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
            SshLocalBridge.init();
            if (!showInfo()) {
                return;
            }

            var host = "localhost";
            var b = SshLocalBridge.get();
            var port = b.getPort();
            var user = b.getUser();
            var name = b.getIdentityKey().getFileName().toString();
            Hyperlinks.open("termius://app/host-sharing#label=" + name + "&ip=" + host + "&port=" + port + "&username="
                    + user + "&os=undefined");
        }

        private boolean showInfo() throws IOException {
            boolean set = AppCache.getBoolean("termiusSetup", false);
            if (set) {
                return true;
            }

            var b = SshLocalBridge.get();
            var keyContent = Files.readString(b.getIdentityKey());
            var r = AppWindowHelper.showBlockingAlert(alert -> {
                alert.setTitle(AppI18n.get("termiusSetup"));
                alert.setAlertType(Alert.AlertType.NONE);

                var activated = AppI18n.get()
                        .getMarkdownDocumentation("app:termiusSetup")
                        .formatted(b.getIdentityKey(), keyContent);
                var markdown = new MarkdownComp(activated, s -> s)
                        .prefWidth(450)
                        .prefHeight(450)
                        .createRegion();
                alert.getDialogPane().setContent(markdown);

                alert.getButtonTypes().add(new ButtonType(AppI18n.get("ok"), ButtonBar.ButtonData.OK_DONE));
            });
            r.filter(buttonType -> buttonType.getButtonData().isDefaultButton());
            r.ifPresent(buttonType -> {
                AppCache.update("termiusSetup", true);
            });
            return r.isPresent();
        }
    };

    ExternalTerminalType CMD = new CmdTerminalType();

    ExternalTerminalType POWERSHELL = new PowerShellTerminalType();

    ExternalTerminalType PWSH = new PwshTerminalType();

    ExternalTerminalType GNOME_TERMINAL = new GnomeTerminalType();

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
    ExternalTerminalType FOOT = new SimplePathType("app.foot", "foot", true) {
        @Override
        public String getWebsite() {
            return "https://codeberg.org/dnkl/foot";
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
                    .add("--title")
                    .addQuoted(configuration.getColoredTitle())
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
            return CommandBuilder.of().add("--new-tab").add("-e").addFile(configuration.getScriptFile());
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
        public int getProcessHierarchyOffset() {
            return 1;
        }

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
        public int getProcessHierarchyOffset() {
            return 1;
        }

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
        public int getProcessHierarchyOffset() {
            return ProcessControlProvider.get().getEffectiveLocalDialect() == ShellDialects.BASH ? 0 : 1;
        }

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
            return CommandBuilder.of().add("-e").add(configuration.getDialectLaunchCommand());
        }
    };
    ExternalTerminalType MACOS_TERMINAL = new MacOsType("app.macosTerminal", "Terminal") {

        @Override
        public int getProcessHierarchyOffset() {
            return 2;
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
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("open", "-a")
                            .addQuoted("Terminal.app")
                            .addFile(configuration.getScriptFile()));
        }
    };
    ExternalTerminalType ITERM2 = new MacOsType("app.iterm2", "iTerm") {

        @Override
        public int getProcessHierarchyOffset() {
            return 3;
        }

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
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("open", "-a")
                            .addQuoted("iTerm.app")
                            .addFile(configuration.getScriptFile()));
        }
    };
    ExternalTerminalType WARP = new MacOsType("app.warp", "Warp") {

        @Override
        public int getProcessHierarchyOffset() {
            return 2;
        }

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
            WindowsTerminalType.WINDOWS_TERMINAL_CANARY,
            WindowsTerminalType.WINDOWS_TERMINAL_PREVIEW,
            WindowsTerminalType.WINDOWS_TERMINAL,
            AlacrittyTerminalType.ALACRITTY_WINDOWS,
            WezTerminalType.WEZTERM_WINDOWS,
            CMD,
            PWSH,
            POWERSHELL,
            MOBAXTERM,
            SECURECRT,
            TERMIUS,
            XSHELL,
            TabbyTerminalType.TABBY_WINDOWS);
    List<ExternalTerminalType> LINUX_TERMINALS = List.of(
            AlacrittyTerminalType.ALACRITTY_LINUX,
            WezTerminalType.WEZTERM_LINUX,
            KittyTerminalType.KITTY_LINUX,
            TERMINATOR,
            TERMINOLOGY,
            XFCE,
            ELEMENTARY,
            KONSOLE,
            GNOME_TERMINAL,
            TILIX,
            GUAKE,
            TILDA,
            XTERM,
            DEEPIN_TERMINAL,
            FOOT,
            Q_TERMINAL,
            TERMIUS);
    List<ExternalTerminalType> MACOS_TERMINALS = List.of(
            WARP,
            ITERM2,
            KittyTerminalType.KITTY_MACOS,
            TabbyTerminalType.TABBY_MAC_OS,
            AlacrittyTerminalType.ALACRITTY_MAC_OS,
            WezTerminalType.WEZTERM_MAC_OS,
            MACOS_TERMINAL,
            TERMIUS);

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

        // Verify that our selection is still valid
        if (existing != null && existing.isAvailable()) {
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
        DataColor color;
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

    abstract class MacOsType extends ExternalApplicationType.MacApplication
            implements ExternalTerminalType, TrackableTerminalType {

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
    abstract class SimplePathType extends PathCheckType implements TrackableTerminalType {

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
