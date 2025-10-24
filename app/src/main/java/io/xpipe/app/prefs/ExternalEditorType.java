package io.xpipe.app.prefs;

import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.OsType;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

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

    WindowsType VSCODIUM_WINDOWS = new WindowsType() {

        @Override
        public String getWebsite() {
            return "https://vscodium.com/";
        }

        @Override
        public String getId() {
            return "app.vscodium";
        }

        @Override
        public boolean detach() {
            return false;
        }

        @Override
        public String getExecutable() {
            return "codium.cmd";
        }

        @Override
        public Optional<Path> determineInstallation() {
            return Optional.of(AppSystemInfo.ofWindows()
                            .getLocalAppData()
                            .resolve("Programs")
                            .resolve("VSCodium")
                            .resolve("bin")
                            .resolve("codium.cmd"))
                    .filter(path -> Files.exists(path));
        }
    };

    WindowsType CURSOR_WINDOWS = new WindowsType() {

        @Override
        public String getWebsite() {
            return "https://cursor.com/";
        }

        @Override
        public String getId() {
            return "app.cursor";
        }

        @Override
        public boolean detach() {
            return true;
        }

        @Override
        public String getExecutable() {
            return "Cursor";
        }

        @Override
        public Optional<Path> determineInstallation() {
            return Optional.of(AppSystemInfo.ofWindows()
                            .getLocalAppData()
                            .resolve("Programs")
                            .resolve("cursor")
                            .resolve("Cursor.exe"))
                    .filter(path -> Files.exists(path));
        }
    };

    WindowsType VOID_WINDOWS = new WindowsType() {

        @Override
        public String getWebsite() {
            return "https://voideditor.com/";
        }

        @Override
        public String getId() {
            return "app.void";
        }

        @Override
        public boolean detach() {
            return true;
        }

        @Override
        public String getExecutable() {
            return "Void";
        }

        @Override
        public Optional<Path> determineInstallation() {
            return Optional.of(AppSystemInfo.ofWindows()
                            .getProgramFiles()
                            .resolve("Void")
                            .resolve("Void.exe"))
                    .filter(path -> Files.exists(path));
        }
    };

    WindowsType WINDSURF_WINDOWS = new WindowsType() {

        @Override
        public String getWebsite() {
            return "https://windsurf.com/editor";
        }

        @Override
        public String getId() {
            return "app.windsurf";
        }

        @Override
        public boolean detach() {
            return false;
        }

        @Override
        public String getExecutable() {
            return "windsurf.cmd";
        }

        @Override
        public Optional<Path> determineInstallation() {
            return Optional.of(AppSystemInfo.ofWindows()
                            .getLocalAppData()
                            .resolve("Programs")
                            .resolve("Windsurf")
                            .resolve("bin")
                            .resolve("windsurf.cmd"))
                    .filter(path -> Files.exists(path));
        }
    };

    WindowsType KIRO_WINDOWS = new WindowsType() {

        @Override
        public String getWebsite() {
            return "https://kiro.dev/";
        }

        @Override
        public String getId() {
            return "app.kiro";
        }

        @Override
        public boolean detach() {
            return false;
        }

        @Override
        public String getExecutable() {
            return "kiro.cmd";
        }

        @Override
        public Optional<Path> determineInstallation() {
            return Optional.of(AppSystemInfo.ofWindows()
                            .getLocalAppData()
                            .resolve("Programs")
                            .resolve("Kiro")
                            .resolve("bin")
                            .resolve("kiro.cmd"))
                    .filter(path -> Files.exists(path));
        }
    };

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

    WindowsType TRAE_WINDOWS = new WindowsType() {

        @Override
        public String getWebsite() {
            return "https://www.trae.ai/";
        }

        @Override
        public String getId() {
            return "app.trae";
        }

        @Override
        public boolean detach() {
            return false;
        }

        @Override
        public String getExecutable() {
            return "trae.cmd";
        }

        @Override
        public Optional<Path> determineInstallation() {
            return Optional.of(AppSystemInfo.ofWindows()
                            .getLocalAppData()
                            .resolve("Programs")
                            .resolve("Trae")
                            .resolve("bin")
                            .resolve("trae.cmd"))
                    .filter(path -> Files.exists(path));
        }
    };

    WindowsType VSCODE_WINDOWS = new WindowsType() {

        @Override
        public String getWebsite() {
            return "https://code.visualstudio.com/";
        }

        @Override
        public String getId() {
            return "app.vscode";
        }

        @Override
        public boolean detach() {
            return false;
        }

        @Override
        public String getExecutable() {
            return "code.cmd";
        }

        @Override
        public Optional<Path> determineInstallation() {
            return Optional.of(AppSystemInfo.ofWindows()
                            .getLocalAppData()
                            .resolve("Programs")
                            .resolve("Microsoft VS Code")
                            .resolve("bin")
                            .resolve("code.cmd"))
                    .filter(path -> Files.exists(path));
        }
    };

    WindowsType VSCODE_INSIDERS_WINDOWS = new WindowsType() {

        @Override
        public String getWebsite() {
            return "https://code.visualstudio.com/insiders/";
        }

        @Override
        public String getId() {
            return "app.vscodeInsiders";
        }

        @Override
        public boolean detach() {
            return false;
        }

        @Override
        public String getExecutable() {
            return "code-insiders.cmd";
        }

        @Override
        public Optional<Path> determineInstallation() {
            return Optional.of(AppSystemInfo.ofWindows()
                            .getLocalAppData()
                            .resolve("Programs")
                            .resolve("Microsoft VS Code Insiders")
                            .resolve("bin")
                            .resolve("code-insiders.cmd"))
                    .filter(path -> Files.exists(path));
        }
    };

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

    LinuxPathType VSCODE_LINUX = new LinuxPathType("app.vscode", "code", "https://code.visualstudio.com/") {
        @Override
        public void launch(Path file) throws Exception {
            var builder = CommandBuilder.of()
                    .fixedEnvironment("DONT_PROMPT_WSL_INSTALL", "No_Prompt_please")
                    .addFile(getExecutable())
                    .addFile(file.toString());
            ExternalApplicationHelper.startAsync(builder);
        }
    };

    LinuxPathType WINDSURF_LINUX = new LinuxPathType("app.windsurf", "windsurf", "https://windsurf.com/editor");

    LinuxPathType CURSOR_LINUX = new LinuxPathType("app.cursor", "cursor", "https://cursor.com/");

    LinuxPathType KIRO_LINUX = new LinuxPathType("app.kiro", "kiro", "https://kiro.dev/");

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

    LinuxPathType ZED_LINUX = new LinuxPathType("app.zed", "zed", "https://zed.dev/");

    ExternalEditorType ZED_MACOS = new MacOsEditor("app.zed", "Zed", "https://zed.dev/");

    LinuxPathType VSCODIUM_LINUX = new LinuxPathType("app.vscodium", "codium", "https://vscodium.com/");

    LinuxPathType GNOME = new LinuxPathType("app.gnomeTextEditor", "gnome-text-editor", "https://vscodium.com/");

    LinuxPathType KATE = new LinuxPathType("app.kate", "kate", "https://kate-editor.org");

    LinuxPathType GEDIT = new LinuxPathType("app.gedit", "gedit", "https://gedit-text-editor.org/");

    LinuxPathType LEAFPAD = new LinuxPathType("app.leafpad", "leafpad", "https://snapcraft.io/leafpad");

    LinuxType MOUSEPAD = new LinuxType("app.mousepad", "mousepad", "https://docs.xfce.org/apps/mousepad/start", "org.xfce.mousepad");

    LinuxPathType PLUMA = new LinuxPathType("app.pluma", "pluma", "https://github.com/mate-desktop/pluma");
    ExternalEditorType TEXT_EDIT =
            new MacOsEditor("app.textEdit", "TextEdit", "https://support.apple.com/en-gb/guide/textedit/welcome/mac");
    ExternalEditorType BBEDIT = new MacOsEditor("app.bbedit", "BBEdit", "https://www.barebones.com/products/bbedit/");
    ExternalEditorType SUBLIME_MACOS = new MacOsEditor("app.sublime", "Sublime Text", "https://www.sublimetext.com/");
    ExternalEditorType VSCODE_MACOS =
            new MacOsEditor("app.vscode", "Visual Studio Code", "https://code.visualstudio.com/");
    ExternalEditorType VSCODIUM_MACOS = new MacOsEditor("app.vscodium", "VSCodium", "https://vscodium.com/");
    ExternalEditorType CURSOR_MACOS = new MacOsEditor("app.cursor", "Cursor", "https://cursor.com/");
    ExternalEditorType VOID_MACOS = new MacOsEditor("app.void", "Void", "https://voideditor.com/");
    ExternalEditorType WINDSURF_MACOS = new MacOsEditor("app.windsurf", "Windsurf", "https://windsurf.com/editor");
    ExternalEditorType KIRO_MACOS = new MacOsEditor("app.kiro", "Kiro", "https://kiro.dev/");
    ExternalEditorType TRAE_MACOS = new MacOsEditor("app.trae", "Trae", "https://www.trae.ai/");
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
            VOID_WINDOWS,
            CURSOR_WINDOWS,
            WINDSURF_WINDOWS,
            TRAE_WINDOWS,
            KIRO_WINDOWS,
            VSCODIUM_WINDOWS,
            VSCODE_INSIDERS_WINDOWS,
            VSCODE_WINDOWS,
            NOTEPADPLUSPLUS,
            NOTEPAD);
    List<GenericPathType> LINUX_EDITORS = List.of(
            ExternalEditorType.WINDSURF_LINUX,
            ExternalEditorType.KIRO_LINUX,
            VSCODIUM_LINUX,
            VSCODE_LINUX,
            ZED_LINUX,
            KATE,
            GEDIT,
            PLUMA,
            LEAFPAD,
            MOUSEPAD,
            GNOME,
            ExternalEditorType.CURSOR_LINUX);
    List<ExternalEditorType> MACOS_EDITORS = List.of(
            VOID_MACOS,
            CURSOR_MACOS,
            WINDSURF_MACOS,
            KIRO_MACOS,
            TRAE_MACOS,
            BBEDIT,
            VSCODIUM_MACOS,
            VSCODE_MACOS,
            SUBLIME_MACOS,
            ZED_MACOS,
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

    interface WindowsType extends ExternalApplicationType.WindowsType, ExternalEditorType {

        @Override
        default void launch(Path file) throws Exception {
            var location = findExecutable();
            var builder = CommandBuilder.of().addFile(location.toString()).addFile(file.toString());
            if (detach()) {
                ExternalApplicationHelper.startAsync(builder);
            } else {
                LocalShell.getShell().executeSimpleCommand(builder);
            }
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
            var exec = CommandSupport.isInLocalPath(getExecutable()) || getFlatpakId() == null ?
                    CommandBuilder.of().addFile(getExecutable()) :
                    CommandBuilder.of().add("flatpak", "run").addQuoted(getFlatpakId());
            var builder = CommandBuilder.of().add(exec).addFile(file.toString());
            if (detach()) {
                ExternalApplicationHelper.startAsync(builder);
            } else {
                LocalShell.getShell().executeSimpleCommand(builder);
            }
        }

        @Override
        public String getFlatpakId() throws Exception {
            return flatpakId;
        }
    }
}
