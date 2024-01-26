package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.ApplicationHelper;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public interface ExternalEditorType extends PrefsChoiceValue {

    ExternalEditorType NOTEPAD = new WindowsType("app.notepad", "notepad") {
        @Override
        protected Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("SystemRoot") + "\\System32\\notepad.exe"));
        }
    };

    ExternalEditorType VSCODIUM_WINDOWS = new WindowsType("app.vscodium", "codium.cmd") {

        @Override
        protected Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                    .resolve("Programs")
                    .resolve("VSCodium")
                    .resolve("bin")
                    .resolve("codium.cmd"));
        }

        @Override
        public boolean detach() {
            return false;
        }
    };

    ExternalEditorType VSCODE_WINDOWS = new WindowsType("app.vscode", "code.cmd") {

        @Override
        protected Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                    .resolve("Programs")
                    .resolve("Microsoft VS Code")
                    .resolve("bin")
                    .resolve("code.cmd"));
        }

        @Override
        public boolean detach() {
            return false;
        }
    };

    ExternalEditorType VSCODE_INSIDERS_WINDOWS = new WindowsType("app.vscodeInsiders", "code-insiders.cmd") {

        @Override
        protected Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                                       .resolve("Programs")
                                       .resolve("Microsoft VS Code Insiders")
                                       .resolve("bin")
                                       .resolve("code-insiders.cmd"));
        }

        @Override
        public boolean detach() {
            return false;
        }
    };

    ExternalEditorType NOTEPADPLUSPLUS_WINDOWS = new WindowsType("app.notepad++", "notepad++") {

        @Override
        protected Optional<Path> determineInstallation() {
            var found = WindowsRegistry.readString(WindowsRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Notepad++", null);

            // Check 32 bit install
            if (found.isEmpty()) {
                found = WindowsRegistry.readString(WindowsRegistry.HKEY_LOCAL_MACHINE, "WOW6432Node\\SOFTWARE\\Notepad++", null);
            }
            return found.map(p -> p + "\\notepad++.exe").map(Path::of);
        }
    };

    LinuxPathType VSCODE_LINUX = new LinuxPathType("app.vscode", "code");

    LinuxPathType VSCODIUM_LINUX = new LinuxPathType("app.vscodium", "codium");

    LinuxPathType GNOME = new LinuxPathType("app.gnomeTextEditor", "gnome-text-editor");

    LinuxPathType KATE = new LinuxPathType("app.kate", "kate");

    LinuxPathType GEDIT = new LinuxPathType("app.gedit", "gedit");

    LinuxPathType LEAFPAD = new LinuxPathType("app.leafpad", "leafpad");

    LinuxPathType MOUSEPAD = new LinuxPathType("app.mousepad", "mousepad");

    LinuxPathType PLUMA = new LinuxPathType("app.pluma", "pluma");

    class MacOsEditor extends ExternalApplicationType.MacApplication implements ExternalEditorType {

        public MacOsEditor(String id, String applicationName) {
            super(id, applicationName);
        }

        @Override
        public void launch(Path file) throws Exception {
            var execFile = getApplicationPath();
            if (execFile.isEmpty()) {
                throw new IOException("Application " + applicationName + ".app not found");
            }

            ApplicationHelper.executeLocalApplication(
                    shellControl -> String.format(
                            "open -a %s %s",
                            shellControl
                                    .getShellDialect()
                                    .fileArgument(execFile.orElseThrow().toString()),
                            shellControl.getShellDialect().fileArgument(file.toString())),
                    false);
        }
    }

    ExternalEditorType TEXT_EDIT = new MacOsEditor("app.textEdit", "TextEdit");

    ExternalEditorType BBEDIT = new MacOsEditor("app.bbedit", "BBEdit");

    ExternalEditorType SUBLIME_MACOS = new MacOsEditor("app.sublime", "Sublime Text");

    ExternalEditorType VSCODE_MACOS = new MacOsEditor("app.vscode", "Visual Studio Code");

    ExternalEditorType VSCODIUM_MACOS = new MacOsEditor("app.vscodium", "Visual Studio Code");

    ExternalEditorType CUSTOM = new ExternalEditorType() {

        @Override
        public void launch(Path file) throws Exception {
            var customCommand = AppPrefs.get().customEditorCommand().getValue();
            if (customCommand == null || customCommand.isBlank()) {
                throw ErrorEvent.unreportable(new IllegalStateException("No custom editor command specified"));
            }

            var format = customCommand.toLowerCase(Locale.ROOT).contains("$file") ? customCommand : customCommand + " $FILE";
            ApplicationHelper.executeLocalApplication(sc -> ApplicationHelper.replaceFileArgument(format, "FILE", file.toString()), true);
        }

        @Override
        public String getId() {
            return "app.custom";
        }
    };

    void launch(Path file) throws Exception;

    class GenericPathType extends ExternalApplicationType.PathApplication implements ExternalEditorType {

        public GenericPathType(String id, String command) {
            super(id, command);
        }

        @Override
        public void launch(Path file) throws Exception {
            LocalShell.getShell().executeSimpleCommand(CommandBuilder.of().add(executable).addFile(file.toString()));
        }

        @Override
        public boolean isSelectable() {
            return true;
        }
    }

    class LinuxPathType extends GenericPathType {

        public LinuxPathType(String id, String command) {
            super(id, command);
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    }

    abstract class WindowsType extends ExternalApplicationType.WindowsType
            implements ExternalEditorType {

        private final String executable;

        public WindowsType(String id, String executable) {
            super(id, executable);
            this.executable = executable;
        }

        public boolean detach() {
            return true;
        }

        @Override
        public void launch(Path file) throws Exception {
            var location = determineFromPath();
            if (location.isEmpty()) {
                location = determineInstallation();
                if (location.isEmpty()) {
                    throw ErrorEvent.unreportable(new IOException("Unable to find installation of " + toTranslatedString()));
                }
            }

            Optional<Path> finalLocation = location;
            ApplicationHelper.executeLocalApplication(
                    sc -> String.format(
                            "%s %s",
                            sc.getShellDialect().fileArgument(finalLocation.get().toString()),
                            sc.getShellDialect().fileArgument(file.toString())),
                    detach());
        }
    }

    ExternalEditorType FLEET = new GenericPathType("app.fleet", "fleet");
    ExternalEditorType INTELLIJ = new GenericPathType("app.intellij", "idea");
    ExternalEditorType PYCHARM = new GenericPathType("app.pycharm", "pycharm");
    ExternalEditorType WEBSTORM = new GenericPathType("app.webstorm", "webstorm");
    ExternalEditorType CLION = new GenericPathType("app.clion", "clion");

    List<ExternalEditorType> WINDOWS_EDITORS = List.of(VSCODIUM_WINDOWS, VSCODE_INSIDERS_WINDOWS, VSCODE_WINDOWS, NOTEPADPLUSPLUS_WINDOWS, NOTEPAD);
    List<LinuxPathType> LINUX_EDITORS = List.of(ExternalEditorType.VSCODIUM_LINUX, VSCODE_LINUX, KATE, GEDIT, PLUMA, LEAFPAD, MOUSEPAD, GNOME);
    List<ExternalEditorType> MACOS_EDITORS = List.of(BBEDIT, VSCODIUM_MACOS, VSCODE_MACOS, SUBLIME_MACOS, TEXT_EDIT);
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

    static void detectDefault() {
        var typeProperty = AppPrefs.get().externalEditor;
        var customProperty = AppPrefs.get().customEditorCommand;
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            typeProperty.set(WINDOWS_EDITORS.stream()
                    .filter(externalEditorType -> externalEditorType.isAvailable())
                    .findFirst()
                    .orElse(null));
        }

        if (OsType.getLocal().equals(OsType.LINUX)) {
            var env = System.getenv("VISUAL");
            if (env != null) {
                var found = LINUX_EDITORS.stream()
                        .filter(externalEditorType -> externalEditorType.executable.equalsIgnoreCase(env))
                        .findFirst()
                        .orElse(null);
                if (found == null) {
                    typeProperty.set(CUSTOM);
                    customProperty.set(env);
                } else {
                    typeProperty.set(found);
                }
            } else {
                typeProperty.set(LINUX_EDITORS.stream()
                        .filter(externalEditorType -> externalEditorType.isAvailable())
                        .findFirst()
                        .orElse(null));
            }
        }

        if (OsType.getLocal().equals(OsType.MACOS)) {
            typeProperty.set(MACOS_EDITORS.stream()
                    .filter(externalEditorType -> externalEditorType.isAvailable())
                    .findFirst()
                    .orElse(null));
        }
    }
}
