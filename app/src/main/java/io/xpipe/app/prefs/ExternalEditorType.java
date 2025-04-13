package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellScript;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public interface ExternalEditorType extends PrefsChoiceValue {

    ExternalEditorType NOTEPAD = new WindowsType("app.notepad", "notepad", false) {
        @Override
        protected Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("SystemRoot") + "\\System32\\notepad.exe"));
        }
    };

    ExternalEditorType VSCODIUM_WINDOWS = new WindowsType("app.vscodium", "codium.cmd", false) {

        @Override
        protected Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                    .resolve("Programs")
                    .resolve("VSCodium")
                    .resolve("bin")
                    .resolve("codium.cmd"));
        }
    };

    WindowsType CURSOR_WINDOWS = new WindowsType("app.cursor", "Cursor", true) {

        @Override
        protected Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                    .resolve("Programs")
                    .resolve("cursor")
                    .resolve("Cursor.exe"));
        }
    };

    WindowsType WINDSURF_WINDOWS = new WindowsType("app.windsurf", "windsurf.cmd", false) {

        @Override
        protected Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                    .resolve("Programs")
                    .resolve("Windsurf")
                    .resolve("bin")
                    .resolve("windsurf.cmd"));
        }
    };

    // Cli is broken, keep inactive
    WindowsType THEIAIDE_WINDOWS = new WindowsType("app.theiaide", "Theiaide", true) {

        @Override
        protected Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                    .resolve("Programs")
                    .resolve("TheiaIDE")
                    .resolve("TheiaIDE.exe"));
        }
    };

    WindowsType TRAE_WINDOWS = new WindowsType("app.trae", "trae.cmd", false) {

        @Override
        protected Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                    .resolve("Programs")
                    .resolve("Trae")
                    .resolve("bin")
                    .resolve("trae.cmd"));
        }
    };

    WindowsType VSCODE_WINDOWS = new WindowsType("app.vscode", "code.cmd", false) {

        @Override
        protected Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                    .resolve("Programs")
                    .resolve("Microsoft VS Code")
                    .resolve("bin")
                    .resolve("code.cmd"));
        }
    };

    ExternalEditorType VSCODE_INSIDERS_WINDOWS = new WindowsType("app.vscodeInsiders", "code-insiders.cmd", false) {

        @Override
        protected Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                    .resolve("Programs")
                    .resolve("Microsoft VS Code Insiders")
                    .resolve("bin")
                    .resolve("code-insiders.cmd"));
        }
    };

    ExternalEditorType NOTEPADPLUSPLUS = new WindowsType("app.notepad++", "notepad++", false) {

        @Override
        protected Optional<Path> determineInstallation() {
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
                    .addFile(executable)
                    .addFile(file.toString());
            ExternalApplicationHelper.startAsync(builder);
        }
    };

    LinuxPathType WINDSURF_LINUX = new LinuxPathType("app.windsurf", "windsurf");

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
    ExternalEditorType WINDSURF_MACOS = new MacOsEditor("app.windsurf", "Windsurf");
    ExternalEditorType TRAE_MACOS = new MacOsEditor("app.trae", "Trae");
    ExternalEditorType CUSTOM = new ExternalEditorType() {

        @Override
        public void launch(Path file) throws Exception {
            var customCommand = AppPrefs.get().customEditorCommand().getValue();
            if (customCommand == null || customCommand.isBlank()) {
                throw ErrorEvent.expected(new IllegalStateException("No custom editor command specified"));
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
            CURSOR_WINDOWS,
            WINDSURF_WINDOWS,
            TRAE_WINDOWS,
            VSCODIUM_WINDOWS,
            VSCODE_INSIDERS_WINDOWS,
            VSCODE_WINDOWS,
            NOTEPADPLUSPLUS,
            NOTEPAD);
    List<LinuxPathType> LINUX_EDITORS = List.of(
            ExternalEditorType.WINDSURF_LINUX,
            VSCODIUM_LINUX,
            VSCODE_LINUX,
            ZED_LINUX,
            KATE,
            GEDIT,
            PLUMA,
            LEAFPAD,
            MOUSEPAD,
            GNOME);
    List<ExternalEditorType> MACOS_EDITORS = List.of(
            CURSOR_MACOS,
            WINDSURF_MACOS,
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

    class MacOsEditor extends ExternalApplicationType.MacApplication implements ExternalEditorType {

        public MacOsEditor(String id, String applicationName) {
            super(id, applicationName);
        }

        @Override
        public void launch(Path file) throws Exception {
            try (var sc = LocalShell.getShell().start()) {
                sc.executeSimpleCommand(CommandBuilder.of()
                        .add("open", "-a")
                        .addQuoted(applicationName)
                        .addFile(file.toString()));
            }
        }
    }

    class GenericPathType extends ExternalApplicationType.PathApplication implements ExternalEditorType {

        public GenericPathType(String id, String command, boolean explicityAsync) {
            super(id, command, explicityAsync);
        }

        @Override
        public void launch(Path file) throws Exception {
            var builder = CommandBuilder.of().addFile(executable).addFile(file.toString());
            if (explicitlyAsync) {
                ExternalApplicationHelper.startAsync(builder);
            } else {
                LocalShell.getShell().executeSimpleCommand(builder);
            }
        }
    }

    class LinuxPathType extends GenericPathType {

        public LinuxPathType(String id, String command) {
            super(id, command, true);
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    }

    abstract class WindowsType extends ExternalApplicationType.WindowsType implements ExternalEditorType {

        private final boolean detach;

        public WindowsType(String id, String executable, boolean detach) {
            super(id, executable);
            this.detach = detach;
        }

        @Override
        public void launch(Path file) throws Exception {
            var location = findExecutable();
            if (location.isEmpty()) {
                throw ErrorEvent.expected(new IOException(
                        "Unable to find installation of " + toTranslatedString().getValue()));
            }

            var builder = CommandBuilder.of().addFile(location.get().toString()).addFile(file.toString());
            if (detach) {
                ExternalApplicationHelper.startAsync(builder);
            } else {
                LocalShell.getShell().executeSimpleCommand(builder);
            }
        }

        public Optional<Path> findExecutable() {
            var location = determineFromPath();
            if (location.isEmpty()) {
                location = determineInstallation();
            }
            return location;
        }
    }
}
