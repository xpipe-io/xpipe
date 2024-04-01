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
import java.util.Comparator;
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

    public static class MacApplication extends ExternalApplicationType {

        protected final String applicationName;

        public MacApplication(String id, String applicationName) {
            super(id);
            this.applicationName = applicationName;
        }

        protected Optional<Path> getApplicationPath() {
            try (ShellControl pc = LocalShell.getShell().start()) {
                try (var c = pc.command(String.format(
                                "/System/Library/Frameworks/CoreServices.framework/Versions/A/Frameworks/LaunchServices.framework/Versions/A/Support/lsregister "
                                        + "-dump | grep -o \"/.*%s.app\" | grep -v -E \"Caches|TimeMachine|Temporary|.Trash|/Volumes/%s\" | uniq",
                                applicationName, applicationName))
                        .start()) {
                    var path = c.readStdoutDiscardErr();
                    if (c.getExitCode() != 0 || path.isBlank()) {
                        return Optional.empty();
                    }

                    // Check if returned paths are actually valid
                    // Also sort them by length to prevent finding a deeply buried app
                    var valid = path.lines()
                            .filter(s -> {
                                try {
                                    return Files.exists(Path.of(s));
                                } catch (Exception ex) {
                                    return false;
                                }
                            })
                            .sorted(Comparator.comparingInt(value -> value.length()))
                            .toList();

                    // Require app in proper applications directory
                    var app = valid.stream()
                            .filter(s -> s.contains("Applications"))
                            .findFirst();
                    return app.map(Path::of);
                }
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
                return Optional.empty();
            }
        }

        @Override
        public boolean isAvailable() {
            return getApplicationPath().isPresent();
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.MACOS);
        }
    }

    public abstract static class PathApplication extends ExternalApplicationType {

        protected final String executable;
        protected final boolean explicityAsync;

        public PathApplication(String id, String executable, boolean explicityAsync) {
            super(id);
            this.executable = executable;
            this.explicityAsync = explicityAsync;
        }

        public boolean isAvailable() {
            try (ShellControl pc = LocalShell.getShell()) {
                return pc.executeSimpleBooleanCommand(pc.getShellDialect().getWhichCommand(executable));
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
                if (explicityAsync) {
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
            try (var cc = LocalShell.getShell()
                    .command(CommandBuilder.ofFunction(
                            var1 -> var1.getShellDialect().getWhichCommand(executable)))
                    .start()) {
                var out = cc.readStdoutDiscardErr();
                var exit = cc.getExitCode();
                if (exit == 0) {
                    var first = out.lines().findFirst();
                    if (first.isPresent()) {
                        return first.map(String::trim).map(Path::of);
                    }
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
