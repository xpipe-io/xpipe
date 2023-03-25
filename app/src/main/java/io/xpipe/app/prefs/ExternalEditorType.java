package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.util.ApplicationHelper;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.process.OsType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public interface ExternalEditorType extends PrefsChoiceValue {

    public static final ExternalEditorType NOTEPAD = new WindowsFullPathType("app.notepad") {
        @Override
        protected Optional<Path> determinePath() {
            return Optional.of(Path.of(System.getenv("SystemRoot") + "\\System32\\notepad.exe"));
        }
    };

    public static final ExternalEditorType VSCODE = new WindowsFullPathType("app.vscode") {

        @Override
        protected Optional<Path> determinePath() {
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                    .resolve("Programs")
                    .resolve("Microsoft VS Code")
                    .resolve("bin")
                    .resolve("code.cmd"));
        }
    };
    public static final ExternalEditorType NOTEPADPLUSPLUS_WINDOWS = new WindowsFullPathType("app.notepad++") {

        @Override
        protected Optional<Path> determinePath() {
            Optional<String> launcherDir;
            launcherDir = WindowsRegistry.readString(WindowsRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Notepad++", null)
                    .map(p -> p + "\\notepad++.exe");
            return launcherDir.map(Path::of);
        }
    };

    public static final LinuxPathType VSCODE_LINUX = new LinuxPathType("app.vscode", "code");

    public static final LinuxPathType KATE = new LinuxPathType("app.kate", "kate");

    public static final LinuxPathType GEDIT = new LinuxPathType("app.gedit", "gedit");

    public static final LinuxPathType LEAFPAD = new LinuxPathType("app.leafpad", "leafpad");

    public static final LinuxPathType MOUSEPAD = new LinuxPathType("app.mousepad", "mousepad");

    public static final LinuxPathType PLUMA = new LinuxPathType("app.pluma", "pluma");

    class MacOsEditor extends ExternalApplicationType.MacApplication implements ExternalEditorType {

        public MacOsEditor(String id, String applicationName) {
            super(id, applicationName);
        }

        @Override
        public void launch(Path file) throws Exception {
            ApplicationHelper.executeLocalApplication(
                    shellControl -> String.format(
                            "open -a %s %s",
                            shellControl.getShellDialect().fileArgument(getApplicationPath().orElseThrow().toString()),
                            shellControl.getShellDialect().fileArgument(file.toString())),
                    false);
        }
    }

    public static final ExternalEditorType TEXT_EDIT = new MacOsEditor("app.textEdit", "TextEdit");

    public static final ExternalEditorType SUBLIME_MACOS = new MacOsEditor("app.sublime", "Sublime Text");

    public static final ExternalEditorType VSCODE_MACOS = new MacOsEditor("app.vscode", "Visual Studio Code");

    public static final ExternalEditorType CUSTOM = new ExternalEditorType() {

        @Override
        public void launch(Path file) throws Exception {
            var customCommand = AppPrefs.get().customEditorCommand().getValue();
            if (customCommand == null || customCommand.isBlank()) {
                throw new IllegalStateException("No custom editor command specified");
            }

            var format = customCommand.contains("$file") ? customCommand : customCommand + " $file";
            var fileString = file.toString().contains(" ") ? "\"" + file + "\"" : file.toString();
            ApplicationHelper.executeLocalApplication(sc -> format.replace("$file", fileString), true);
        }

        @Override
        public boolean isSelectable() {
            return true;
        }

        @Override
        public String getId() {
            return "app.custom";
        }
    };

    public void launch(Path file) throws Exception;

    public static class LinuxPathType extends ExternalApplicationType.PathApplication implements ExternalEditorType {

        public LinuxPathType(String id, String command) {
            super(id, command);
        }

        @Override
        public void launch(Path file) throws IOException {
            new ProcessBuilder(List.of(executable, file.toString())).start();
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    }

    public abstract static class WindowsFullPathType extends ExternalApplicationType.WindowsFullPathType
            implements ExternalEditorType {

        public WindowsFullPathType(String id) {
            super(id);
        }

        @Override
        public void launch(Path file) throws Exception {
            var path = determinePath();
            if (path.isEmpty()) {
                throw new IOException("Unable to find installation of " + getId());
            }

            ApplicationHelper.executeLocalApplication(
                    sc -> String.format(
                            "%s %s", sc.getShellDialect().fileArgument(path.get().toString()), sc.getShellDialect().fileArgument(file.toString())),
                    true);
        }
    }

    public static final List<ExternalEditorType> WINDOWS_EDITORS = List.of(VSCODE, NOTEPADPLUSPLUS_WINDOWS, NOTEPAD);
    public static final List<LinuxPathType> LINUX_EDITORS =
            List.of(VSCODE_LINUX, KATE, GEDIT, PLUMA, LEAFPAD, MOUSEPAD);
    public static final List<ExternalEditorType> MACOS_EDITORS = List.of(VSCODE_MACOS, SUBLIME_MACOS, TEXT_EDIT);

    public static final List<ExternalEditorType> ALL = ((Supplier<List<ExternalEditorType>>) () -> {
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
                all.add(CUSTOM);
                return all;
            })
            .get();

    public static void detectDefault() {
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
