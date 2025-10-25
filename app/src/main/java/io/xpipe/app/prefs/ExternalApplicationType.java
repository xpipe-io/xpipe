package io.xpipe.app.prefs;

import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.PrefsValue;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandControl;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.FlatpakCache;
import io.xpipe.app.util.Translatable;
import io.xpipe.core.OsType;

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
                    AppSystemInfo.ofCurrent().getUserHome().resolve("Applications", getApplicationName() + ".app");
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
            return OsType.ofLocal() == OsType.MACOS;
        }
    }

    interface PathApplication extends ExternalApplicationType {

        String getExecutable();

        boolean detach();

        default boolean isAvailable() {
            try (ShellControl pc = LocalShell.getShell()) {
                String name = getExecutable();
                return pc.view().findProgram(name).isPresent();
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).omit().handle();
                return false;
            }
        }

        default void launch(CommandBuilder args) throws Exception {
            try (ShellControl pc = LocalShell.getShell()) {
                String executable = getExecutable();
                if (!pc.view().findProgram(executable).isPresent()) {
                    throw ErrorEventFactory.expected(new IOException("Executable " + getExecutable()
                            + " not found in PATH. Either add it to the PATH and refresh the environment by restarting XPipe, or specify an absolute "
                            + "executable path using the custom terminal setting."));
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

    interface LinuxApplication extends PathApplication {

        String getFlatpakId() throws Exception;

        @Override
        default void launch(CommandBuilder args) throws Exception {
            if (getFlatpakId() == null
                    || LocalShell.getShell().view().findProgram(getExecutable()).isPresent()) {
                PathApplication.super.launch(args);
                return;
            }

            var app = FlatpakCache.getApp(getFlatpakId());
            if (app.isEmpty()) {
                throw ErrorEventFactory.expected(new IOException(
                        "Executable " + getExecutable() + " not found in PATH nor as a flatkpak " + getFlatpakId()
                                + " not installed. Install it and refresh the environment by restarting XPipe"));
            }

            try (ShellControl pc = LocalShell.getShell()) {
                args.add(0, FlatpakCache.runCommand(getFlatpakId()));
                pc.command(args).execute();
            }
        }
    }

    interface InstallLocationType extends ExternalApplicationType {

        String getExecutable();

        Optional<Path> determineInstallation();

        default Optional<Path> determineFromPath() {
            // Try to locate if it is in the Path
            try (var sc = LocalShell.getShell().start()) {
                String name = getExecutable();
                var out = sc.view().findProgram(name);
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
            return OsType.ofLocal() == OsType.WINDOWS;
        }
    }
}
