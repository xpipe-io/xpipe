package io.xpipe.app.prefs;

import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellTypes;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.prefs.PrefsChoiceValue;
import io.xpipe.extension.util.ApplicationHelper;
import io.xpipe.extension.util.WindowsRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@AllArgsConstructor
public abstract class ExternalEditorType implements PrefsChoiceValue {

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
            Optional<String> launcherDir = Optional.empty();
            if (SystemUtils.IS_OS_WINDOWS) {
                launcherDir = WindowsRegistry.readString(
                                WindowsRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Notepad++", null)
                        .map(p -> p + "\\notepad++.exe");
            }

            return launcherDir.map(Path::of);
        }
    };

    public static final PathType NOTEPADPLUSPLUS_LINUX = new PathType("app.notepad++", "notepad++");

    public static final PathType VSCODE_LINUX = new PathType("app.vscode", "code");

    public static final PathType KATE = new PathType("app.kate", "kate");

    public static final PathType GEDIT = new PathType("app.gedit", "gedit");

    public static final PathType LEAFPAD = new PathType("app.leafpad", "leafpad");

    public static final PathType MOUSEPAD = new PathType("app.mousepad", "mousepad");

    public static final PathType PLUMA = new PathType("app.pluma", "pluma");

    public static final ExternalEditorType TEXT_EDIT = new MacOsFullPathType("app.textEdit") {
        @Override
        protected Path determinePath() {
            return Path.of("/Applications/TextEdit.app");
        }
    };

    public static final ExternalEditorType NOTEPADPP_MACOS = new MacOsFullPathType("app.notepad++") {
        @Override
        protected Path determinePath() {
            return Path.of("/Applications/TextEdit.app");
        }
    };

    public static final ExternalEditorType SUBLIME_MACOS = new MacOsFullPathType("app.sublime") {
        @Override
        protected Path determinePath() {
            return Path.of("/Applications/Sublime.app");
        }
    };

    public static final ExternalEditorType VSCODE_MACOS = new MacOsFullPathType("app.vscode") {
        @Override
        protected Path determinePath() {
            return Path.of("/Applications/VSCode.app");
        }
    };

    public static final ExternalEditorType CUSTOM = new ExternalEditorType("app.custom") {

        @Override
        public void launch(Path file) throws Exception {
            var customCommand = AppPrefs.get().customEditorCommand().getValue();
            if (customCommand == null || customCommand.trim().isEmpty()) {
                return;
            }

            var format = customCommand.contains("$file") ? customCommand : customCommand + " $file";
            var fileString = file.toString().contains(" ") ? "\"" + file + "\"" : file.toString();
            ApplicationHelper.executeLocalApplication(format.replace("$file",fileString));
        }

        @Override
        public boolean isSelectable() {
            return true;
        }
    };

    private String id;

    public abstract void launch(Path file) throws Exception;

    public abstract boolean isSelectable();

    public static class PathType extends ExternalEditorType {

        private final String command;

        public PathType(String id, String command) {
            super(id);
            this.command = command;
        }

        @Override
        public void launch(Path file) throws IOException {
            var list = ShellTypes.getPlatformDefault().executeCommandListWithShell(command + " \"" + file + "\"");
            new ProcessBuilder(list).start();
        }

        public boolean isAvailable() {
            try (ShellProcessControl pc = ShellStore.local().create().start()) {
                return pc.executeBooleanSimpleCommand(pc.getShellType().getWhichCommand(command));
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
                return false;
            }
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    }

    public abstract static class WindowsFullPathType extends ExternalEditorType {

        public WindowsFullPathType(String id) {
            super(id);
        }

        protected abstract Optional<Path> determinePath();

        @Override
        public void launch(Path file) throws Exception {
            var path = determinePath();
            if (path.isEmpty()) {
                throw new IOException("Unable to find installation of " + getId());
            }

            ApplicationHelper.executeLocalApplication(List.of(path.get().toString(), file.toString()));
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }

        @Override
        public boolean isAvailable() {
            var path = determinePath();
            return path.isPresent() && Files.exists(path.get());
        }
    }

    public abstract static class MacOsFullPathType extends ExternalEditorType {

        public MacOsFullPathType(String id) {
            super(id);
        }

        protected abstract Path determinePath();

        @Override
        public void launch(Path file) throws Exception {
            var path = determinePath();
            ApplicationHelper.executeLocalApplication(List.of("open", path.toString(), file.toString()));
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.MAC);
        }

        @Override
        public boolean isAvailable() {
            var path = determinePath();
            return Files.exists(path);
        }
    }

    public static final List<ExternalEditorType> WINDOWS_EDITORS = List.of(VSCODE, NOTEPADPLUSPLUS_WINDOWS, NOTEPAD);
    public static final List<PathType> LINUX_EDITORS =
            List.of(VSCODE_LINUX, NOTEPADPLUSPLUS_LINUX, KATE, GEDIT, PLUMA, LEAFPAD, MOUSEPAD);
    public static final List<ExternalEditorType> MACOS_EDITORS =
            List.of(VSCODE_MACOS, SUBLIME_MACOS, NOTEPADPP_MACOS, TEXT_EDIT);

    public static final List<ExternalEditorType> ALL = new ArrayList<>();
    static {
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            ALL.addAll(WINDOWS_EDITORS);
        }
        if (OsType.getLocal().equals(OsType.LINUX)) {
            ALL.addAll(LINUX_EDITORS);
        }
        if (OsType.getLocal().equals(OsType.MAC)) {
            ALL.addAll(MACOS_EDITORS);
        }
        ALL.add(CUSTOM);
    }

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
                        .filter(externalEditorType -> externalEditorType.command.equalsIgnoreCase(env))
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

        if (OsType.getLocal().equals(OsType.MAC)) {
            typeProperty.set(MACOS_EDITORS.stream()
                    .filter(externalEditorType -> externalEditorType.isAvailable())
                    .findFirst()
                    .orElse(null));
        }
    }
}
