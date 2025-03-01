package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public abstract class ExternalApplicationType implements PrefsChoiceValue {

    private final String id;

    public ExternalApplicationType(String id) {
        this.id = id;
    }

    public abstract boolean isAvailable();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return getId();
    }

    public abstract static class MacApplication extends ExternalApplicationType {

        protected final String applicationName;

        public MacApplication(String id, String applicationName) {
            super(id);
            this.applicationName = applicationName;
        }

        @Override
        public boolean isAvailable() {
            try (ShellControl pc = LocalShell.getShell().start()) {
                var out = pc.command(String.format(
                                "mdfind -literal 'kMDItemFSName = \"%s.app\"' -onlyin /Applications -onlyin ~/Applications -onlyin /System/Applications",
                                applicationName))
                        .readStdoutIfPossible();
                return out.isPresent() && !out.get().isBlank() && out.get().contains(applicationName + ".app");
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).handle();
                return false;
            }
        }

        public void focus() {
            try (ShellControl pc = LocalShell.getShell().start()) {
                pc.command(String.format("open -a \"%s.app\"", applicationName)).execute();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).handle();
            }
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.MACOS);
        }
    }

    public abstract static class PathApplication extends ExternalApplicationType {

        protected final String executable;
        protected final boolean explicitlyAsync;

        public PathApplication(String id, String executable, boolean explicitlyAsync) {
            super(id);
            this.executable = executable;
            this.explicitlyAsync = explicitlyAsync;
        }

        public boolean isAvailable() {
            try (ShellControl pc = LocalShell.getShell()) {
                return CommandSupport.findProgram(pc, executable).isPresent();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
                return false;
            }
        }

        protected void launch(String title, CommandBuilder args) throws Exception {
            try (ShellControl pc = LocalShell.getShell()) {
                if (!CommandSupport.isInPath(pc, executable)) {
                    throw ErrorEvent.expected(
                            new IOException(
                                    "Executable " + executable
                                            + " not found in PATH. Either add it to the PATH and refresh the environment by restarting XPipe, or specify an absolute executable path using the custom terminal setting."));
                }

                args.add(0, executable);
                if (explicitlyAsync) {
                    ExternalApplicationHelper.startAsync(args);
                } else {
                    pc.executeSimpleCommand(args);
                }
            }
        }
    }

    public abstract static class WindowsType extends ExternalApplicationType {

        private final String executable;

        public WindowsType(String id, String executable) {
            super(id);
            this.executable = executable;
        }

        protected abstract Optional<Path> determineInstallation();

        protected Optional<Path> determineFromPath() {
            // Try to locate if it is in the Path
            try (var sc = LocalShell.getShell().start()) {
                var out = CommandSupport.findProgram(sc, executable);
                if (out.isPresent()) {
                    return out.map(filePath -> Path.of(filePath.toString()));
                }
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).omit().handle();
            }
            return Optional.empty();
        }

        @Override
        public boolean isAvailable() {
            var path = determineFromPath();
            if (path.isPresent() && Files.exists(path.get())) {
                return true;
            }

            var installation = determineInstallation();
            return installation.isPresent() && Files.exists(installation.get());
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    }
}
