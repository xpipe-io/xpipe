package io.xpipe.app.prefs;

import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.*;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.FlatpakCache;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.app.util.OsType;

import io.xpipe.app.webtop.WebtopApp;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import lombok.AllArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public interface ExternalEditorType extends PrefsChoiceValue {
    ExternalEditorType NOTEPAD = new WindowsType() {

        @Override
        public String getWebsite() {
            return "https://apps.microsoft.com/detail/9msmlrh6lzf3?hl=en-US&gl=US";
        }

        @Override
        public String getId() {
            return "app.notepad";
        }

        @Override
        public boolean detach() {
            return false;
        }

        @Override
        public String getExecutable() {
            return "notepad";
        }

        @Override
        public Optional<Path> determineInstallation() {
            return Optional.of(AppSystemInfo.ofWindows().getSystemRoot().resolve("\\System32\\notepad.exe"));
        }
    };

    WindowsType VSCODIUM_WINDOWS = new VsCodeWindowsType(
            "app.vscodium",
            "https://vscodium.com/",
            () -> {
                var perUser = AppSystemInfo.ofWindows()
                        .getLocalAppData()
                        .resolve("Programs")
                        .resolve("VSCodium");
                if (Files.exists(perUser)) {
                    return perUser;
                }

                var perMachine = AppSystemInfo.ofWindows()
                        .getProgramFiles()
                        .resolve("VSCodium");
                return perMachine;
            },
            "VSCodium.exe");

    WindowsType ANTIGRAVITY_WINDOWS = new VsCodeWindowsType(
            "app.antigravity",
            "https://antigravity.google/",
            () -> {
                var perUser = AppSystemInfo.ofWindows()
                        .getLocalAppData()
                        .resolve("Programs")
                        .resolve("Antigravity");
                if (Files.exists(perUser)) {
                    return perUser;
                }

                var perMachine = AppSystemInfo.ofWindows()
                        .getProgramFiles()
                        .resolve("Antigravity");
                return perMachine;
            },
            "Antigravity.exe");

    WindowsType CURSOR_WINDOWS = new VsCodeWindowsType(
            "app.cursor",
            "https://cursor.com/",
            () -> {
                var perUser = AppSystemInfo.ofWindows()
                        .getLocalAppData()
                        .resolve("Programs")
                        .resolve("cursor");
                if (Files.exists(perUser)) {
                    return perUser;
                }

                var perMachine = AppSystemInfo.ofWindows()
                        .getProgramFiles()
                        .resolve("cursor");
                return perMachine;
            },
            "Cursor.exe");

    WindowsType VOID_WINDOWS = new VsCodeWindowsType(
            "app.void",
            "https://voideditor.com/",
            () -> {
                var perUser = AppSystemInfo.ofWindows()
                        .getLocalAppData()
                        .resolve("Programs")
                        .resolve("Void");
                if (Files.exists(perUser)) {
                    return perUser;
                }

                var perMachine = AppSystemInfo.ofWindows()
                        .getProgramFiles()
                        .resolve("Void");
                return perMachine;
            },
            "Void.exe");

    WindowsType WINDSURF_WINDOWS = new VsCodeWindowsType(
            "app.windsurf",
            "https://windsurf.com/editor",
            () -> {
                var perUser = AppSystemInfo.ofWindows()
                        .getLocalAppData()
                        .resolve("Programs")
                        .resolve("Windsurf");
                if (Files.exists(perUser)) {
                    return perUser;
                }

                var perMachine = AppSystemInfo.ofWindows()
                        .getProgramFiles()
                        .resolve("Windsurf");
                return perMachine;
            },
            "Windsurf.exe");

    WindowsType KIRO_WINDOWS = new VsCodeWindowsType(
            "app.kiro",
            "https://kiro.dev/",
            () -> {
                var perUser = AppSystemInfo.ofWindows()
                        .getLocalAppData()
                        .resolve("Programs")
                        .resolve("Kiro");
                if (Files.exists(perUser)) {
                    return perUser;
                }

                var perMachine = AppSystemInfo.ofWindows()
                        .getProgramFiles()
                        .resolve("Kiro");
                return perMachine;
            },
            "Kiro.exe");

    // Cli is broken, keep inactive
    @SuppressWarnings("unused")
    WindowsType THEIAIDE_WINDOWS = new WindowsType() {

        @Override
        public String getWebsite() {
            return "https://theia-ide.org/";
        }

        @Override
        public String getId() {
            return "app.theiaide";
        }

        @Override
        public boolean detach() {
            return true;
        }

        @Override
        public String getExecutable() {
            return "Theiaide";
        }

        @Override
        public Optional<Path> determineInstallation() {
            return Optional.of(AppSystemInfo.ofWindows()
                            .getLocalAppData()
                            .resolve("Programs")
                            .resolve("TheiaIDE")
                            .resolve("TheiaIDE.exe"))
                    .filter(path -> Files.exists(path));
        }
    };

    WindowsType TRAE_WINDOWS = new VsCodeWindowsType(
            "app.trae",
            "https://www.trae.ai/",
            () -> {
                var perUser = AppSystemInfo.ofWindows()
                        .getLocalAppData()
                        .resolve("Programs")
                        .resolve("Trae");
                if (Files.exists(perUser)) {
                    return perUser;
                }

                var perMachine = AppSystemInfo.ofWindows()
                        .getProgramFiles()
                        .resolve("Trae");
                return perMachine;
            },
            "Trae.exe");

    WindowsType VSCODE_WINDOWS = new VsCodeWindowsType(
            "app.vscode",
            "https://code.visualstudio.com/",
            () -> {
                var perUser = AppSystemInfo.ofWindows()
                        .getLocalAppData()
                        .resolve("Programs")
                        .resolve("Microsoft VS Code");
                if (Files.exists(perUser)) {
                    return perUser;
                }

                var perMachine = AppSystemInfo.ofWindows()
                        .getProgramFiles()
                        .resolve("Microsoft VS Code");
                return perMachine;
            },
            "Code.exe");

    WindowsType VSCODE_INSIDERS_WINDOWS = new VsCodeWindowsType(
            "app.vscodeInsiders",
            "https://code.visualstudio.com/insiders/",
            () -> {
                var perUser = AppSystemInfo.ofWindows()
                        .getLocalAppData()
                        .resolve("Programs")
                        .resolve("Microsoft VS Code Insiders");
                if (Files.exists(perUser)) {
                    return perUser;
                }

                var perMachine = AppSystemInfo.ofWindows()
                        .getProgramFiles()
                        .resolve("Microsoft VS Code Insiders");
                return perMachine;
            },
            "Code - Insiders.exe");

    ExternalEditorType NOTEPADPLUSPLUS = new WindowsType() {

        @Override
        public String getWebsite() {
            return "https://notepad-plus-plus.org/";
        }

        @Override
        public String getId() {
            return "app.notepad++";
        }

        @Override
        public boolean detach() {
            return false;
        }

        @Override
        public String getExecutable() {
            return "notepad++";
        }

        @Override
        public Optional<Path> determineInstallation() {
            var found = WindowsRegistry.local()
                    .readStringValueIfPresent(WindowsRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Notepad++", null);

            // Check 32 bit install
            if (found.isEmpty()) {
                found = WindowsRegistry.local()
                        .readStringValueIfPresent(
                                WindowsRegistry.HKEY_LOCAL_MACHINE, "WOW6432Node\\SOFTWARE\\Notepad++", null);
            }
            return found.map(p -> p + "\\notepad++.exe").map(Path::of);
        }
    };

    LinuxType VSCODE_LINUX =
            new LinuxType("app.vscode", "code", "https://code.visualstudio.com/", "com.visualstudio.code") {

                @Override
                public WebtopApp getRequiredWebtopApp() {
                    return WebtopApp.VSCODE;
                }

                @Override
                public void launch(Path file) throws Exception {
                    var exec = CommandSupport.isInLocalPath(getExecutable())
                            ? CommandBuilder.of().addFile(getExecutable())
                            : FlatpakCache.getRunCommand(getFlatpakId());

                    if (FlatpakCache.getApp(getFlatpakId()).isEmpty()) {
                        CommandSupport.isInPathOrThrow(LocalShell.getShell(), getExecutable());
                    }

                    var builder = CommandBuilder.of()
                            .fixedEnvironment("DONT_PROMPT_WSL_INSTALL", "No_Prompt_please")
                            .add(exec)
                            .addFile(file.toString());
                    ExternalApplicationHelper.startAsync(builder);
                }
            };

    LinuxPathType WINDSURF_LINUX = new LinuxPathType("app.windsurf", "windsurf", "https://windsurf.com/editor");

    LinuxPathType CURSOR_LINUX = new LinuxPathType("app.cursor", "cursor", "https://cursor.com/");

    LinuxPathType KIRO_LINUX = new LinuxPathType("app.kiro", "kiro", "https://kiro.dev/");

    LinuxType NEOVIM_LINUX = new LinuxType("app.neovim", "nvim", "https://neovim.io/", null) {
        @Override
        public void launch(Path file) throws Exception {
            TerminalLaunch.builder()
                    .title(file.toString())
                    .localScript(sc -> new ShellScript(CommandBuilder.of()
                            .addFile(getExecutable())
                            .addFile(file.toString())
                            .buildFull(sc)))
                    .logIfEnabled(false)
                    .preferTabs(false)
                    .pauseOnExit(false)
                    .launch();
        }
    };

    WindowsType NEOVIM_WINDOWS = new WindowsType() {
        @Override
        public String getId() {
            return "app.neovim";
        }

        @Override
        public boolean detach() {
            return false;
        }

        @Override
        public String getExecutable() {
            return "nvim";
        }

        @Override
        public String getWebsite() {
            return "https://neovim.io/";
        }

        @Override
        public Optional<Path> determineInstallation() {
            var programFiles = AppSystemInfo.ofWindows()
                    .getProgramFiles()
                    .resolve("Neovim", "bin")
                    .resolve("nvim.exe");
            if (Files.exists(programFiles)) {
                return Optional.of(programFiles);
            }
            return Optional.empty();
        }

        @Override
        public void launch(Path file) throws Exception {
            TerminalLaunch.builder()
                    .title(file.toString())
                    .localScript(sc -> new ShellScript(CommandBuilder.of()
                            .addFile(findExecutable().toString())
                            .addFile(file)
                            .buildFull(sc)))
                    .logIfEnabled(false)
                    .preferTabs(false)
                    .pauseOnExit(false)
                    .launch();
        }
    };

    WindowsType ZED_WINDOWS = new WindowsType() {

        @Override
        public String getWebsite() {
            return "https://zed.dev/";
        }

        @Override
        public String getExecutable() {
            return "Zed.exe";
        }

        @Override
        public Optional<Path> determineInstallation() {
            var nightly = AppSystemInfo.ofWindows().getLocalAppData().resolve("Programs", "Zed Nightly", "Zed.exe");
            if (Files.exists(nightly)) {
                return Optional.of(nightly);
            }

            var regular = AppSystemInfo.ofWindows().getLocalAppData().resolve("Programs", "Zed", "Zed.exe");
            if (Files.exists(regular)) {
                return Optional.of(regular);
            }

            return Optional.empty();
        }

        @Override
        public boolean detach() {
            return false;
        }

        @Override
        public String getId() {
            return "app.zed";
        }
    };

    LinuxType ZED_LINUX = new LinuxType("app.zed", "zed", "https://zed.dev/", "dev.zed.Zed");

    ExternalEditorType ZED_MACOS = new MacOsEditor("app.zed", "Zed", "https://zed.dev/");

    LinuxType VSCODIUM_LINUX = new LinuxType("app.vscodium", "codium", "https://vscodium.com/", "com.vscodium.codium");

    LinuxType ANTIGRAVITY_LINUX = new LinuxType("app.antigravity", "antigravity", "https://antigravity.google/", null);

    LinuxType GNOME = new LinuxType("app.gnomeTextEditor", "gnome-text-editor", "LinuxType", "org.gnome.TextEditor");

    LinuxType KATE = new LinuxType("app.kate", "kate", "https://kate-editor.org", "org.kde.kate");

    LinuxType GEDIT = new LinuxType("app.gedit", "gedit", "https://gedit-text-editor.org/", "org.gnome.gedit");

    LinuxPathType LEAFPAD = new LinuxPathType("app.leafpad", "leafpad", "https://snapcraft.io/leafpad");

    LinuxType MOUSEPAD =
            new LinuxType("app.mousepad", "mousepad", "https://docs.xfce.org/apps/mousepad/start", "org.xfce.mousepad");

    LinuxPathType PLUMA = new LinuxPathType("app.pluma", "pluma", "https://github.com/mate-desktop/pluma");
    LinuxPathType COSMIC_EDIT =
            new LinuxPathType("app.cosmicEdit", "cosmic-edit", "https://github.com/pop-os/cosmic-edit");
    LinuxPathType WESTON_EDITOR =
            new LinuxPathType("app.westonEditor", "weston-editor", "https://wayland.pages.freedesktop.org/weston/");
    ExternalEditorType TEXT_EDIT =
            new MacOsEditor("app.textEdit", "TextEdit", "https://support.apple.com/en-gb/guide/textedit/welcome/mac");
    ExternalEditorType BBEDIT = new MacOsEditor("app.bbedit", "BBEdit", "https://www.barebones.com/products/bbedit/");
    ExternalEditorType SUBLIME_MACOS = new MacOsEditor("app.sublime", "Sublime Text", "https://www.sublimetext.com/");
    ExternalEditorType VSCODE_MACOS =
            new MacOsEditor("app.vscode", "Visual Studio Code", "https://code.visualstudio.com/");
    ExternalEditorType VSCODIUM_MACOS = new MacOsEditor("app.vscodium", "VSCodium", "https://vscodium.com/");
    ExternalEditorType ANTIGRAVITY_MACOS =
            new MacOsEditor("app.antigravity", "Antigravity", "https://antigravity.google/");
    ExternalEditorType CURSOR_MACOS = new MacOsEditor("app.cursor", "Cursor", "https://cursor.com/");
    ExternalEditorType VOID_MACOS = new MacOsEditor("app.void", "Void", "https://voideditor.com/");
    ExternalEditorType WINDSURF_MACOS = new MacOsEditor("app.windsurf", "Windsurf", "https://windsurf.com/editor");
    ExternalEditorType KIRO_MACOS = new MacOsEditor("app.kiro", "Kiro", "https://kiro.dev/");
    ExternalEditorType TRAE_MACOS = new MacOsEditor("app.trae", "Trae", "https://www.trae.ai/");
    ExternalEditorType NEOVIM_MACOS = new MacOsEditor("app.neovim", "Neovim", "https://neovim.io/") {
        @Override
        public void launch(Path file) throws Exception {
            TerminalLaunch.builder()
                    .title(file.toString())
                    .localScript(sc -> new ShellScript(CommandBuilder.of()
                            .addFile("nvim")
                            .addFile(file.toString())
                            .buildFull(sc)))
                    .logIfEnabled(false)
                    .preferTabs(false)
                    .pauseOnExit(false)
                    .launch();
        }
    };
    ExternalEditorType CUSTOM = new ExternalEditorType() {

        @Override
        public String getWebsite() {
            return null;
        }

        @Override
        public void launch(Path file) throws Exception {
            var customCommand = AppPrefs.get().customEditorCommand().getValue();
            if (customCommand == null || customCommand.isBlank()) {
                throw ErrorEventFactory.expected(new IllegalStateException("No custom editor command specified"));
            }

            var format =
                    customCommand.toLowerCase(Locale.ROOT).contains("$file") ? customCommand : customCommand + " $FILE";
            var command = CommandBuilder.of()
                    .add(ExternalApplicationHelper.replaceVariableArgument(format, "FILE", file.toString()));
            if (AppPrefs.get().customEditorCommandInTerminal().get()) {
                TerminalLaunch.builder()
                        .title(file.toString())
                        .localScript(sc -> new ShellScript(command.buildFull(sc)))
                        .logIfEnabled(false)
                        .preferTabs(false)
                        .pauseOnExit(false)
                        .launch();
            } else {
                ExternalApplicationHelper.startAsync(command);
            }
        }

        @Override
        public ObservableValue<String> toTranslatedString() {
            var customCommand = AppPrefs.get().customEditorCommand().getValue();
            if (customCommand == null
                    || customCommand.isBlank()
                    || customCommand.replace("$FILE", "").strip().contains(" ")) {
                return ExternalEditorType.super.toTranslatedString();
            }

            return new SimpleStringProperty(customCommand);
        }

        @Override
        public String getId() {
            return "app.custom";
        }
    };
    ExternalEditorType FLEET = new GenericPathType("app.fleet", "fleet", false, "https://www.jetbrains.com/fleet/");
    ExternalEditorType INTELLIJ = new GenericPathType("app.intellij", "idea", false, "https://www.jetbrains.com/idea/");
    ExternalEditorType PYCHARM =
            new GenericPathType("app.pycharm", "pycharm", false, "https://www.jetbrains.com/pycharm/");
    ExternalEditorType WEBSTORM =
            new GenericPathType("app.webstorm", "webstorm", false, "https://www.jetbrains.com/webstorm/");
    ExternalEditorType CLION = new GenericPathType("app.clion", "clion", false, "https://www.jetbrains.com/clion/");
    List<ExternalEditorType> WINDOWS_EDITORS = List.of(
            ZED_WINDOWS,
            VSCODE_WINDOWS,
            VSCODIUM_WINDOWS,
            NOTEPADPLUSPLUS,
            VOID_WINDOWS,
            CURSOR_WINDOWS,
            WINDSURF_WINDOWS,
            TRAE_WINDOWS,
            KIRO_WINDOWS,
            ANTIGRAVITY_WINDOWS,
            VSCODE_INSIDERS_WINDOWS,
            NOTEPAD,
            NEOVIM_WINDOWS);
    List<GenericPathType> LINUX_EDITORS = List.of(
            ZED_LINUX,
            VSCODE_LINUX,
            KATE,
            GEDIT,
            NEOVIM_LINUX,
            ExternalEditorType.WINDSURF_LINUX,
            ExternalEditorType.KIRO_LINUX,
            VSCODIUM_LINUX,
            ANTIGRAVITY_LINUX,
            PLUMA,
            LEAFPAD,
            MOUSEPAD,
            GNOME,
            ExternalEditorType.COSMIC_EDIT,
            ExternalEditorType.WESTON_EDITOR,
            ExternalEditorType.CURSOR_LINUX);
    List<ExternalEditorType> MACOS_EDITORS = List.of(
            ZED_MACOS,
            VSCODE_MACOS,
            VSCODIUM_MACOS,
            BBEDIT,
            SUBLIME_MACOS,
            VOID_MACOS,
            CURSOR_MACOS,
            WINDSURF_MACOS,
            KIRO_MACOS,
            TRAE_MACOS,
            ANTIGRAVITY_MACOS,
            NEOVIM_MACOS,
            TEXT_EDIT);
    List<ExternalEditorType> CROSS_PLATFORM_EDITORS = List.of(FLEET, INTELLIJ, PYCHARM, WEBSTORM, CLION);

    @SuppressWarnings({"unused", "TrivialFunctionalExpressionUsage"})
    List<ExternalEditorType> ALL = ((Supplier<List<ExternalEditorType>>) () -> {
                var all = new ArrayList<ExternalEditorType>();
                if (OsType.ofLocal() == OsType.WINDOWS) {
                    all.addAll(WINDOWS_EDITORS);
                }
                if (OsType.ofLocal() == OsType.LINUX) {
                    all.addAll(LINUX_EDITORS);
                }
                if (OsType.ofLocal() == OsType.MACOS) {
                    all.addAll(MACOS_EDITORS);
                }
                all.addAll(CROSS_PLATFORM_EDITORS);
                all.add(CUSTOM);
                return all;
            })
            .get();

    static ExternalEditorType determineDefault(ExternalEditorType existing) {
        // Verify that our selection is still valid
        if (existing != null && existing.isAvailable()) {
            return existing;
        }

        if (OsType.ofLocal() == OsType.WINDOWS) {
            return WINDOWS_EDITORS.stream()
                    .filter(PrefsChoiceValue::isAvailable)
                    .findFirst()
                    .orElse(NOTEPAD);
        }

        if (OsType.ofLocal() == OsType.LINUX) {
            return LINUX_EDITORS.stream()
                    .filter(ExternalApplicationType.PathApplication::isAvailable)
                    .findFirst()
                    .orElse(null);
        }

        if (OsType.ofLocal() == OsType.MACOS) {
            return MACOS_EDITORS.stream()
                    .filter(PrefsChoiceValue::isAvailable)
                    .findFirst()
                    .orElse(TEXT_EDIT);
        }

        return null;
    }

    String getWebsite();

    void launch(Path file) throws Exception;

    default WebtopApp getRequiredWebtopApp() {
        return null;
    }

    interface WindowsType extends ExternalApplicationType.WindowsType, ExternalEditorType {

        @Override
        default void launch(Path file) throws Exception {
            var location = findExecutable();
            // Use quotes for file in case editor does not like single quotes or other
            var builder = CommandBuilder.of().addFile(location.toString()).addQuoted(file.toString());
            if (detach()) {
                ExternalApplicationHelper.startAsync(builder);
            } else {
                LocalShell.getShell().command(builder).execute();
            }
        }
    }

    @AllArgsConstructor
    class VsCodeWindowsType implements WindowsType {

        private final String id;
        private final String link;
        private final Supplier<Path> dir;
        private final String exeName;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean detach() {
            // Launching the exe requires detach
            return LocalShell.getDialect() != ShellDialects.CMD;
        }

        @Override
        public String getExecutable() {
            return exeName;
        }

        @Override
        public Optional<Path> determineInstallation() {
            return Optional.of(dir.get().resolve(exeName)).filter(Files::exists);
        }

        @Override
        public String getWebsite() {
            return link;
        }
    }

    class MacOsEditor implements ExternalApplicationType.MacApplication, ExternalEditorType {

        private final String id;
        private final String appName;
        private final String website;

        public MacOsEditor(String id, String appName, String website) {
            this.id = id;
            this.appName = appName;
            this.website = website;
        }

        @Override
        public String getWebsite() {
            return website;
        }

        @Override
        public void launch(Path file) throws Exception {
            try (var sc = LocalShell.getShell().start()) {
                sc.executeSimpleCommand(CommandBuilder.of()
                        .add("open", "-a")
                        .addQuoted(getApplicationName())
                        .addFile(file.toString()));
            }
        }

        @Override
        public String getApplicationName() {
            return appName;
        }

        @Override
        public String getId() {
            return id;
        }
    }

    class GenericPathType implements ExternalApplicationType.PathApplication, ExternalEditorType {

        private final String id;
        private final String executable;
        private final boolean async;
        private final String website;

        public GenericPathType(String id, String executable, boolean async, String website) {
            this.id = id;
            this.executable = executable;
            this.async = async;
            this.website = website;
        }

        @Override
        public String getWebsite() {
            return website;
        }

        @Override
        public void launch(Path file) throws Exception {
            var builder = CommandBuilder.of().addFile(getExecutable()).addFile(file.toString());
            if (detach()) {
                ExternalApplicationHelper.startAsync(builder);
            } else {
                LocalShell.getShell().executeSimpleCommand(builder);
            }
        }

        @Override
        public String getExecutable() {
            return executable;
        }

        @Override
        public boolean detach() {
            return async;
        }

        @Override
        public String getId() {
            return id;
        }
    }

    class LinuxPathType extends GenericPathType {

        public LinuxPathType(String id, String executable, String website) {
            super(id, executable, true, website);
        }

        @Override
        public boolean isSelectable() {
            return OsType.ofLocal() == OsType.LINUX;
        }
    }

    class LinuxType extends GenericPathType implements ExternalApplicationType.LinuxApplication {

        private final String flatpakId;

        public LinuxType(String id, String executable, String website, String flatpakId) {
            super(id, executable, true, website);
            this.flatpakId = flatpakId;
        }

        @Override
        public void launch(Path file) throws Exception {
            if (CommandSupport.isInLocalPath(getExecutable())) {
                var builder = CommandBuilder.of().add(getExecutable()).addFile(file.toString());
                if (detach()) {
                    ExternalApplicationHelper.startAsync(builder);
                } else {
                    LocalShell.getShell().command(builder).execute();
                }
            } else {
                if (flatpakId == null || FlatpakCache.getApp(flatpakId).isEmpty()) {
                    CommandSupport.isInPathOrThrow(LocalShell.getShell(), getExecutable());
                }

                var builder = FlatpakCache.getRunCommand(getFlatpakId()).addFile(file.toString());
                ExternalApplicationHelper.startAsync(builder);
            }
        }

        @Override
        public String getFlatpakId() {
            return flatpakId;
        }
    }
}
