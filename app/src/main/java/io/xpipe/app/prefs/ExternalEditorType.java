package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.LocalShell;
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
            return Optional.of(Path.of(System.getenv("SystemRoot") + "\\System32\\notepad.exe"));
        }
    };

    ExternalEditorType VSCODIUM_WINDOWS = new WindowsType() {

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
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                            .resolve("Programs")
                            .resolve("VSCodium")
                            .resolve("bin")
                            .resolve("codium.cmd"))
                    .filter(path -> Files.exists(path));
        }
    };

    WindowsType CURSOR_WINDOWS = new WindowsType() {

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
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                            .resolve("Programs")
                            .resolve("cursor")
                            .resolve("Cursor.exe"))
                    .filter(path -> Files.exists(path));
        }
    };

    WindowsType VOID_WINDOWS = new WindowsType() {

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
            return Optional.of(Path.of(System.getenv("PROGRAMFILES"))
                            .resolve("Void")
                            .resolve("Void.exe"))
                    .filter(path -> Files.exists(path));
        }
    };

    WindowsType WINDSURF_WINDOWS = new WindowsType() {

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
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                            .resolve("Programs")
                            .resolve("Windsurf")
                            .resolve("bin")
                            .resolve("windsurf.cmd"))
                    .filter(path -> Files.exists(path));
        }
    };

    WindowsType KIRO_WINDOWS = new WindowsType() {

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
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                            .resolve("Programs")
                            .resolve("Kiro")
                            .resolve("bin")
                            .resolve("kiro.cmd"))
                    .filter(path -> Files.exists(path));
        }
    };

    // Cli is broken, keep inactive
    WindowsType THEIAIDE_WINDOWS = new WindowsType() {

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
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                            .resolve("Programs")
                            .resolve("TheiaIDE")
                            .resolve("TheiaIDE.exe"))
                    .filter(path -> Files.exists(path));
        }
    };

    WindowsType TRAE_WINDOWS = new WindowsType() {

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
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                            .resolve("Programs")
                            .resolve("Trae")
                            .resolve("bin")
                            .resolve("trae.cmd"))
                    .filter(path -> Files.exists(path));
        }
    };

    WindowsType VSCODE_WINDOWS = new WindowsType() {

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
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                            .resolve("Programs")
                            .resolve("Microsoft VS Code")
                            .resolve("bin")
                            .resolve("code.cmd"))
                    .filter(path -> Files.exists(path));
        }
    };

    ExternalEditorType VSCODE_INSIDERS_WINDOWS = new WindowsType() {

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
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                            .resolve("Programs")
                            .resolve("Microsoft VS Code Insiders")
                            .resolve("bin")
                            .resolve("code-insiders.cmd"))
                    .filter(path -> Files.exists(path));
        }
    };

    ExternalEditorType NOTEPADPLUSPLUS = new WindowsType() {

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

    LinuxPathType VSCODE_LINUX = new LinuxPathType("app.vscode", "code") {
        @Override
        public void launch(Path file) throws Exception {
            var builder = CommandBuilder.of()
                    .fixedEnvironment("DONT_PROMPT_WSL_INSTALL", "No_Prompt_please")
                    .addFile(getExecutable())
                    .addFile(file.toString());
            ExternalApplicationHelper.startAsync(builder);
        }
    };

    LinuxPathType WINDSURF_LINUX = new LinuxPathType("app.windsurf", "windsurf");

    LinuxPathType CURSOR_LINUX = new LinuxPathType("app.cursor", "cursor");

    LinuxPathType KIRO_LINUX = new LinuxPathType("app.kiro", "kiro");

    LinuxPathType ZED_LINUX = new LinuxPathType("app.zed", "zed");

    ExternalEditorType ZED_MACOS = new MacOsEditor("app.zed", "Zed");

    LinuxPathType VSCODIUM_LINUX = new LinuxPathType("app.vscodium", "codium");

    LinuxPathType GNOME = new LinuxPathType("app.gnomeTextEditor", "gnome-text-editor");

    LinuxPathType KATE = new LinuxPathType("app.kate", "kate");

    LinuxPathType GEDIT = new LinuxPathType("app.gedit", "gedit");

    LinuxPathType LEAFPAD = new LinuxPathType("app.leafpad", "leafpad");

    LinuxPathType MOUSEPAD = new LinuxPathType("app.mousepad", "mousepad");

    LinuxPathType PLUMA = new LinuxPathType("app.pluma", "pluma");
    ExternalEditorType TEXT_EDIT = new MacOsEditor("app.textEdit", "TextEdit");
    ExternalEditorType BBEDIT = new MacOsEditor("app.bbedit", "BBEdit");
    ExternalEditorType SUBLIME_MACOS = new MacOsEditor("app.sublime", "Sublime Text");
    ExternalEditorType VSCODE_MACOS = new MacOsEditor("app.vscode", "Visual Studio Code");
    ExternalEditorType VSCODIUM_MACOS = new MacOsEditor("app.vscodium", "VSCodium");
    ExternalEditorType CURSOR_MACOS = new MacOsEditor("app.cursor", "Cursor");
    ExternalEditorType VOID_MACOS = new MacOsEditor("app.void", "Void");
    ExternalEditorType WINDSURF_MACOS = new MacOsEditor("app.windsurf", "Windsurf");
    ExternalEditorType KIRO_MACOS = new MacOsEditor("app.kiro", "Kiro");
    ExternalEditorType TRAE_MACOS = new MacOsEditor("app.trae", "Trae");
    ExternalEditorType CUSTOM = new ExternalEditorType() {

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
                TerminalLauncher.openDirect(file.toString(), sc -> new ShellScript(command.buildFull(sc)));
            } else {
                ExternalApplicationHelper.startAsync(command);
            }
        }

        @Override
        public String getId() {
            return "app.custom";
        }
    };
    ExternalEditorType FLEET = new GenericPathType("app.fleet", "fleet", false);
    ExternalEditorType INTELLIJ = new GenericPathType("app.intellij", "idea", false);
    ExternalEditorType PYCHARM = new GenericPathType("app.pycharm", "pycharm", false);
    ExternalEditorType WEBSTORM = new GenericPathType("app.webstorm", "webstorm", false);
    ExternalEditorType CLION = new GenericPathType("app.clion", "clion", false);
    List<ExternalEditorType> WINDOWS_EDITORS = List.of(
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
    List<LinuxPathType> LINUX_EDITORS = List.of(
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

    @SuppressWarnings("TrivialFunctionalExpressionUsage")
    List<ExternalEditorType> ALL = ((Supplier<List<ExternalEditorType>>) () -> {
                var all = new ArrayList<ExternalEditorType>();
                if (OsType.getLocal().equals(OsType.WINDOWS)) {
                    all.addAll(WINDOWS_EDITORS);
                }
                if (OsType.getLocal().equals(OsType.LINUX)) {
                    all.addAll(LINUX_EDITORS);
                }
                if (OsType.getLocal().equals(OsType.MACOS)) {
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

        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            return WINDOWS_EDITORS.stream()
                    .filter(PrefsChoiceValue::isAvailable)
                    .findFirst()
                    .orElse(NOTEPAD);
        }

        if (OsType.getLocal().equals(OsType.LINUX)) {
            return LINUX_EDITORS.stream()
                    .filter(ExternalApplicationType.PathApplication::isAvailable)
                    .findFirst()
                    .orElse(null);
        }

        if (OsType.getLocal().equals(OsType.MACOS)) {
            return MACOS_EDITORS.stream()
                    .filter(PrefsChoiceValue::isAvailable)
                    .findFirst()
                    .orElse(TEXT_EDIT);
        }

        return null;
    }

    void launch(Path file) throws Exception;

    class MacOsEditor implements ExternalApplicationType.MacApplication, ExternalEditorType {

        private final String id;
        private final String appName;

        public MacOsEditor(String id, String appName) {
            this.id = id;
            this.appName = appName;
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

        public GenericPathType(String id, String executable, boolean async) {
            this.id = id;
            this.executable = executable;
            this.async = async;
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

        public LinuxPathType(String id, String executable) {
            super(id, executable, true);
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    }

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
}
