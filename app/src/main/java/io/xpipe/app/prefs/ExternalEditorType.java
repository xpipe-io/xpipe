package io.xpipe.app.prefs;

import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellTypes;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.prefs.PrefsChoiceValue;
import io.xpipe.extension.util.ApplicationHelper;
import io.xpipe.extension.util.WindowsRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.nio.file.Path;
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
    public static final ExternalEditorType NOTEPADPLUSPLUS_WINDOWS = new WindowsFullPathType("app.notepad++Windows") {

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
    public static final ExternalEditorType NOTEPADPLUSPLUS_LINUX =
            new LinuxPathType("app.notepad++Linux", "notepad++") {};

    public static final ExternalEditorType KATE = new LinuxPathType("app.kate", "kate") {};

    public static final ExternalEditorType CUSTOM = new ExternalEditorType("app.custom") {

        @Override
        public void launch(Path file) throws IOException {
            var fileName = SystemUtils.IS_OS_WINDOWS ? " \"" + file + "\"" : file;
            var cmd = AppPrefs.get().customEditorCommand().getValue();
            var fullCmd = cmd + " " + fileName;
            Runtime.getRuntime()
                    .exec(ShellTypes.getPlatformDefault()
                            .executeCommandListWithShell(fullCmd)
                            .toArray(String[]::new));
        }

        @Override
        public boolean isSupported() {
            return true;
        }
    };

    public static final ExternalEditorType TEXT_EDIT = new ExternalEditorType("app.textEdit") {

        @Override
        public void launch(Path file) throws Exception {
            var fullCmd = "/Applications/TextEdit.app/Contents/MacOS/TextEdit \"" + file.toString() + "\"";
            ShellStore.withLocal(pc -> {
                pc.executeSimpleCommand(fullCmd);
            });
        }

        @Override
        public boolean isSupported() {
            return OsType.getLocal().equals(OsType.MAC);
        }
    };
    public static final List<ExternalEditorType> ALL =
            List.of(NOTEPAD, NOTEPADPLUSPLUS_WINDOWS, NOTEPADPLUSPLUS_LINUX, KATE, TEXT_EDIT, CUSTOM);
    private String id;

    public static ExternalEditorType getDefault() {
        if (OsType.getLocal().equals(OsType.MAC)) {
            return TEXT_EDIT;
        }

        return OsType.getLocal().equals(OsType.WINDOWS) ? NOTEPAD : KATE;
    }

    public abstract void launch(Path file) throws Exception;

    public abstract boolean isSupported();

    public abstract static class LinuxPathType extends ExternalEditorType {

        private final String command;

        public LinuxPathType(String id, String command) {
            super(id);
            this.command = command;
        }

        @Override
        public void launch(Path file) throws IOException {
            var list = ShellTypes.getPlatformDefault().executeCommandListWithShell(command + " \"" + file + "\"");
            new ProcessBuilder(list).start();
        }

        @Override
        public boolean isSupported() {
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

            ApplicationHelper.executeLocalApplication(getCommand(path.get(), file));
        }

        protected String getCommand(Path p, Path file) {
            var cmd = "\"" + p + "\"";
            return "start \"\" " + cmd + " \"" + file + "\"";
        }

        @Override
        public boolean isSupported() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    }
}
