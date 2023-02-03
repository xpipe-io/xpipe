package io.xpipe.app.prefs;

import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.prefs.PrefsChoiceValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public abstract class ExternalApplicationType implements PrefsChoiceValue {

    private final String id;

    public ExternalApplicationType(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public abstract boolean isSelectable();

    public abstract boolean isAvailable();

    public static class MacApplication extends ExternalApplicationType {

        protected final String applicationName;

        public MacApplication(String id, String applicationName) {
            super(id);
            this.applicationName = applicationName;
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.MAC);
        }

        @Override
        public boolean isAvailable() {
            try {
                return ShellStore.local()
                        .create()
                        .executeBooleanSimpleCommand(String.format("mdfind -name '%s.app'", applicationName));
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
                return false;
            }
        }
    }

    public static class LinuxPathApplication extends ExternalApplicationType {

        protected final String command;

        public LinuxPathApplication(String id, String command) {
            super(id);
            this.command = command;
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

    public abstract static class WindowsFullPathType extends ExternalApplicationType {

        public WindowsFullPathType(String id) {
            super(id);
        }

        protected abstract Optional<Path> determinePath();

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
}
