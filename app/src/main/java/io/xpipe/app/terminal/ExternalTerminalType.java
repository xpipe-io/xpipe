package io.xpipe.app.terminal;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.core.process.*;

import lombok.Getter;

import java.util.*;

public interface ExternalTerminalType extends PrefsChoiceValue {

    //    ExternalTerminalType PUTTY = new WindowsType("app.putty","putty") {
    //
    //        @Override
    //        public Optional<Path> determineInstallation() {
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

    static ExternalTerminalType determineFallbackTerminalToOpen(ExternalTerminalType type) {
        if (type != null
                && type != XSHELL
                && type != MOBAXTERM
                && type != SECURECRT
                && type != TERMIUS
                && !(type instanceof WaveTerminalType)) {
            return type;
        }

        // Fallback to an available default
        switch (OsType.getLocal()) {
            case OsType.Linux linux -> {
                // This should not be termius or wave as all others take precedence
                var def = determineDefault(null);
                // If there's no other terminal available, use a fallback which won't work
                return def != TERMIUS && def != WaveTerminalType.WAVE_LINUX ? def : XTERM;
            }
            case OsType.MacOs macOs -> {
                return MACOS_TERMINAL;
            }
            case OsType.Windows windows -> {
                return ProcessControlProvider.get().getEffectiveLocalDialect() == ShellDialects.CMD ? CMD : POWERSHELL;
            }
        }
    }

    ExternalTerminalType XSHELL = new XShellTerminalType();

    ExternalTerminalType SECURECRT = new SecureCrtTerminalType();

    ExternalTerminalType MOBAXTERM = new MobaXTermTerminalType();

    ExternalTerminalType TERMIUS = new TermiusTerminalType();

    ExternalTerminalType CMD = new CmdTerminalType();

    ExternalTerminalType POWERSHELL = new PowerShellTerminalType();

    ExternalTerminalType PWSH = new PwshTerminalType();

    ExternalTerminalType GNOME_TERMINAL = new GnomeTerminalType();

    ExternalTerminalType GNOME_CONSOLE = new GnomeConsoleType();

    ExternalTerminalType PTYXIS = new PtyxisTerminalType();

    ExternalTerminalType KONSOLE = new KonsoleTerminalType();
    ExternalTerminalType XFCE = new SimplePathType("app.xfce", "xfce4-terminal", true) {
        @Override
        public String getWebsite() {
            return "https://docs.xfce.org/apps/terminal/start";
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean useColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .addIf(configuration.isPreferTabs(), "--tab")
                    .add("--title")
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
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW;
        }

        @Override
        public boolean isRecommended() {
            return AppPrefs.get().terminalMultiplexer().getValue() != null;
        }

        @Override
        public boolean useColoredTitle() {
            return true;
        }

        @Override
        public boolean supportsEscapes() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
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
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean useColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .addIf(configuration.isPreferTabs(), "--new-tab")
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType TILIX = new SimplePathType("app.tilix", "tilix", true) {
        @Override
        public String getWebsite() {
            return "https://gnunn1.github.io/tilix-web/";
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean useColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
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
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean useColoredTitle() {
            return true;
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-e")
                    .addFile(configuration.getScriptFile())
                    .add("-T")
                    .addQuoted(configuration.getColoredTitle())
                    .addIf(configuration.isPreferTabs(), "--new-tab");
        }
    };
    ExternalTerminalType TERMINOLOGY = new SimplePathType("app.terminology", "terminology", true) {
        @Override
        public String getWebsite() {
            return "https://github.com/borisfaure/terminology";
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean useColoredTitle() {
            return true;
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .addIf(!configuration.isPreferTabs(), "-s")
                    .add("-T")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-2")
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType GHOSTTY = new SimplePathType("app.ghostty", "ghostty", true) {
        @Override
        public String getWebsite() {
            return "https://ghostty.org";
        }

        @Override
        public boolean isRecommended() {
            return AppPrefs.get().terminalMultiplexer().getValue() != null;
        }

        @Override
        public boolean useColoredTitle() {
            return true;
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.TABBED;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of().add("-e").addFile(configuration.getScriptFile());
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
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean useColoredTitle() {
            return true;
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.TABBED;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
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
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean useColoredTitle() {
            return true;
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.TABBED;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of().add("-c").addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType COSMIC_TERM = new SimplePathType("app.cosmicTerm", "cosmic-term", true) {
        @Override
        public String getWebsite() {
            return "https://github.com/pop-os/cosmic-term";
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW;
        }

        @Override
        public boolean isRecommended() {
            return AppPrefs.get().terminalMultiplexer().getValue() != null;
        }

        @Override
        public boolean useColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of().add("-e").addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType UXTERM = new SimplePathType("app.uxterm", "uxterm", true) {
        @Override
        public String getWebsite() {
            return "https://invisible-island.net/xterm/";
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean useColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-title")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType XTERM = new SimplePathType("app.xterm", "xterm", true) {
        @Override
        public String getWebsite() {
            return "https://invisible-island.net/xterm/";
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean useColoredTitle() {
            return false;
        }

        @Override
        public boolean supportsUnicode() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
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
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW;
        }

        @Override
        public boolean isRecommended() {
            return AppPrefs.get().terminalMultiplexer().getValue() != null;
        }

        @Override
        public boolean useColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
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
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW;
        }

        @Override
        public boolean isRecommended() {
            return AppPrefs.get().terminalMultiplexer().getValue() != null;
        }

        @Override
        public boolean useColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of().add("-e").add(configuration.getDialectLaunchCommand());
        }
    };
    ExternalTerminalType MACOS_TERMINAL = new MacOsTerminalType();
    ExternalTerminalType ITERM2 = new ITerm2TerminalType();
    ExternalTerminalType CUSTOM = new CustomTerminalType();
    List<ExternalTerminalType> WINDOWS_TERMINALS = List.of(
            WindowsTerminalType.WINDOWS_TERMINAL_CANARY,
            WindowsTerminalType.WINDOWS_TERMINAL_PREVIEW,
            WindowsTerminalType.WINDOWS_TERMINAL,
            AlacrittyTerminalType.ALACRITTY_WINDOWS,
            WezTerminalType.WEZTERM_WINDOWS,
            WarpTerminalType.WINDOWS,
            CMD,
            PWSH,
            POWERSHELL,
            MOBAXTERM,
            SECURECRT,
            TERMIUS,
            XSHELL,
            TabbyTerminalType.TABBY_WINDOWS,
            WaveTerminalType.WAVE_WINDOWS);
    List<ExternalTerminalType> LINUX_TERMINALS = List.of(
            AlacrittyTerminalType.ALACRITTY_LINUX,
            WezTerminalType.WEZTERM_LINUX,
            KittyTerminalType.KITTY_LINUX,
            GNOME_CONSOLE,
            PTYXIS,
            TERMINATOR,
            TERMINOLOGY,
            XFCE,
            ELEMENTARY,
            KONSOLE,
            GNOME_TERMINAL,
            GHOSTTY,
            TILIX,
            GUAKE,
            TILDA,
            COSMIC_TERM,
            UXTERM,
            XTERM,
            DEEPIN_TERMINAL,
            FOOT,
            Q_TERMINAL,
            WarpTerminalType.LINUX,
            TERMIUS,
            WaveTerminalType.WAVE_LINUX);
    List<ExternalTerminalType> MACOS_TERMINALS = List.of(
            WarpTerminalType.MACOS,
            ITERM2,
            KittyTerminalType.KITTY_MACOS,
            TabbyTerminalType.TABBY_MAC_OS,
            AlacrittyTerminalType.ALACRITTY_MAC_OS,
            WezTerminalType.WEZTERM_MAC_OS,
            MACOS_TERMINAL,
            TERMIUS,
            WaveTerminalType.WAVE_MAC_OS);

    List<ExternalTerminalType> ALL = getTypes(OsType.getLocal(), true);

    List<ExternalTerminalType> ALL_ON_ALL_PLATFORMS = getTypes(null, true);

    static List<ExternalTerminalType> getTypes(OsType osType, boolean custom) {
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

        if (existing == null && AppDistributionType.get() == AppDistributionType.WEBTOP) {
            return ExternalTerminalType.KONSOLE;
        }

        var r = ALL.stream()
                .filter(externalTerminalType -> !externalTerminalType.equals(CUSTOM))
                .filter(terminalType -> terminalType.isAvailable())
                .findFirst()
                .orElse(null);

        // Check if detection failed for some reason
        if (r == null) {
            var def = OsType.getLocal() == OsType.WINDOWS
                    ? (ProcessControlProvider.get().getEffectiveLocalDialect() == ShellDialects.CMD
                            ? ExternalTerminalType.CMD
                            : ExternalTerminalType.POWERSHELL)
                    : OsType.getLocal() == OsType.MACOS ? ExternalTerminalType.MACOS_TERMINAL : null;
            r = def;
        }

        return r;
    }

    default TerminalInitFunction additionalInitCommands() {
        return TerminalInitFunction.none();
    }

    TerminalOpenFormat getOpenFormat();

    default String getWebsite() {
        return null;
    }

    boolean isRecommended();

    boolean useColoredTitle();

    default boolean supportsEscapes() {
        return true;
    }

    default boolean supportsUnicode() {
        return true;
    }

    default boolean shouldClear() {
        return true;
    }

    void launch(TerminalLaunchConfiguration configuration) throws Exception;

    abstract class SimplePathType implements ExternalApplicationType.PathApplication, TrackableTerminalType {

        @Getter
        private final String id;

        @Getter
        private final String executable;

        private final boolean async;

        public SimplePathType(String id, String executable, boolean async) {
            this.id = id;
            this.executable = executable;
            this.async = async;
        }

        @Override
        public boolean detach() {
            return async;
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            var args = toCommand(configuration);
            launch(args);
        }

        protected abstract CommandBuilder toCommand(TerminalLaunchConfiguration configuration);
    }
}
