package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsValue;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.Translatable;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.CommandControl;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public interface ExternalApplicationType extends PrefsValue {

    boolean isAvailable();

    interface MacApplication extends ExternalApplicationType {

        default CommandControl launchCommand(CommandBuilder builder, boolean args) {
            if (args) {
                builder.add(0, "--args");
            }
            builder.addQuoted(0, getApplicationName());
            builder.add(0, "open", "-a");
            return LocalShell.getShell().command(builder);
        }

        @Override
        default boolean isAvailable() {
            try {
                return findApp().isPresent();
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).handle();
                return false;
            }
        }

        String getApplicationName();

        default Optional<Path> findApp() throws Exception {
            // Perform a quick check because mdfind is slow
            var applicationsDef = Path.of("/Applications/" + getApplicationName() + ".app");
            if (Files.exists(applicationsDef)) {
                return Optional.of(applicationsDef);
            }
            var systemApplicationsDef = Path.of("/System/Applications/" + getApplicationName() + ".app");
            if (Files.exists(systemApplicationsDef)) {
                return Optional.of(systemApplicationsDef);
            }
            var userApplicationsDef =
                    Path.of(System.getProperty("user.home") + "/Applications/" + getApplicationName() + ".app");
            if (Files.exists(userApplicationsDef)) {
                return Optional.of(userApplicationsDef);
            }

            try (ShellControl pc = LocalShell.getShell().start()) {
                var out = pc.command(String.format(
                                "mdfind -literal 'kMDItemFSName = \"%s.app\"' -onlyin /Applications -onlyin ~/Applications -onlyin /System/Applications",
                                getApplicationName()))
                        .readStdoutIfPossible();
                return out.isPresent() && !out.get().isBlank() && out.get().contains(getApplicationName() + ".app")
                        ? out.map(s -> Path.of(s))
                        : Optional.empty();
            }
        }

        default void focus() {
            try (ShellControl pc = LocalShell.getShell().start()) {
                pc.command(String.format("open -a \"%s.app\"", getApplicationName()))
                        .execute();
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).handle();
            }
        }

        @Override
        default boolean isSelectable() {
            return OsType.getLocal().equals(OsType.MACOS);
        }
    }

    interface PathApplication extends ExternalApplicationType {

        String getExecutable();

        boolean detach();

        default boolean isAvailable() {
            try (ShellControl pc = LocalShell.getShell()) {
                return CommandSupport.findProgram(pc, getExecutable()).isPresent();
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).omit().handle();
                return false;
            }
        }

        default void launch(CommandBuilder args) throws Exception {
            try (ShellControl pc = LocalShell.getShell()) {
                if (!CommandSupport.isInPath(pc, getExecutable())) {
                    throw ErrorEventFactory.expected(
                            new IOException(
                                    "Executable " + getExecutable()
                                            + " not found in PATH. Either add it to the PATH and refresh the environment by restarting XPipe, or specify an absolute executable path using the custom terminal setting."));
                }

                args.add(0, getExecutable());
                if (detach()) {
                    ExternalApplicationHelper.startAsync(args);
                } else {
                    pc.executeSimpleCommand(args);
                }
            }
        }
    }

    interface InstallLocationType extends ExternalApplicationType {

        String getExecutable();

        Optional<Path> determineInstallation();

        default Optional<Path> determineFromPath() {
            // Try to locate if it is in the Path
            try (var sc = LocalShell.getShell().start()) {
                var out = CommandSupport.findProgram(sc, getExecutable());
                if (out.isPresent()) {
                    return out.map(filePath -> Path.of(filePath.toString()));
                }
            } catch (Exception ex) {
                ErrorEventFactory.fromThrowable(ex).omit().handle();
            }
            return Optional.empty();
        }

        default Path findExecutable() {
            var location = determineFromPath();
            if (location.isEmpty()) {
                location = determineInstallation();
                if (location.isEmpty()) {
                    var name = this instanceof Translatable t
                            ? t.toTranslatedString().getValue()
                            : getExecutable();
                    throw ErrorEventFactory.expected(
                            new UnsupportedOperationException("Unable to find installation of " + name));
                }
            }
            return location.get();
        }

        @Override
        default boolean isAvailable() {
            var path = determineFromPath();
            if (path.isPresent() && Files.exists(path.get())) {
                return true;
            }

            var installation = determineInstallation();
            return installation.isPresent() && Files.exists(installation.get());
        }
    }

    interface WindowsType extends InstallLocationType {

        boolean detach();

        default void launch(CommandBuilder builder) throws Exception {
            var location = findExecutable();
            builder.add(0, sc -> sc.getShellDialect().fileArgument(location.toString()));
            if (detach()) {
                ExternalApplicationHelper.startAsync(builder);
            } else {
                LocalShell.getShell().executeSimpleCommand(builder);
            }
        }

        @Override
        default boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }
    }
}
