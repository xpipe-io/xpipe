package io.xpipe.app.vnc;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public abstract class TigerVncClient implements ExternalVncClient {

    protected CommandBuilder createBuilder(VncLaunchConfig configuration) {
        var builder = CommandBuilder.of().addQuoted(configuration.getHost() + ":" + configuration.getPort());
        builder.addQuotedKeyValue("-ReconnectOnError", "off");
        return builder;
    }

    @Builder
    @Jacksonized
    @JsonTypeName("tigerVnc")
    public static class Windows extends TigerVncClient implements ExternalApplicationType.WindowsType {

        @Override
        public boolean supportsPasswords() {
            return false;
        }

        @Override
        public boolean detach() {
            return false;
        }

        @Override
        public String getExecutable() {
            return "vncviewer.exe";
        }

        @Override
        public Optional<Path> determineFromPath() {
            var found = WindowsType.super.determineFromPath();
            return found.filter(path -> path.toString().contains("TigerVNC"));
        }

        @Override
        public Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("PROGRAMFILES"))
                            .resolve("TigerVNC")
                            .resolve("vncviewer.exe"))
                    .filter(path -> Files.exists(path));
        }

        @Override
        public void launch(VncLaunchConfig configuration) throws Exception {
            var builder = createBuilder(configuration);
            launch(builder);
        }
    }

    @Builder
    @Jacksonized
    @JsonTypeName("tigerVnc")
    public static class Linux extends TigerVncClient implements ExternalApplicationType.PathApplication {

        @Override
        public void launch(VncLaunchConfig configuration) throws Exception {
            var builder = createBuilder(configuration);
            if (configuration.hasFixedPassword()) {
                var pw = configuration.retrievePassword();
                if (pw.isPresent()) {
                    builder.add(sc -> "<(echo "
                            + sc.getShellDialect().literalArgument(pw.get().getSecretValue()) + " | vncpasswd -f)");
                }
            }
            launch(builder);
        }

        @Override
        public boolean supportsPasswords() {
            try {
                return LocalShell.getShell().view().findProgram("vncpasswd").isPresent();
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).handle();
                return false;
            }
        }

        @Override
        public String getExecutable() {
            return "xtigervncviewer";
        }

        @Override
        public boolean detach() {
            return true;
        }
    }

    @Builder
    @Jacksonized
    @JsonTypeName("tigerVnc")
    public static class MacOs extends TigerVncClient implements ExternalApplicationType.InstallLocationType {

        @Override
        public void launch(VncLaunchConfig configuration) throws Exception {
            var loc = findExecutable();
            var builder = createBuilder(configuration);
            var open = CommandBuilder.of().add("open", "-a").addFile(loc).add("--args");
            builder.add(0, open);
            LocalShell.getShell().command(builder).execute();
        }

        @Override
        public boolean supportsPasswords() {
            return false;
        }

        @Override
        public String getExecutable() {
            return "VNCViewer";
        }

        @Override
        public Optional<Path> determineInstallation() {
            try (var appsStream = Files.list(Path.of("/Applications"))) {
                var dirs = appsStream.toList();
                return dirs.stream()
                        .filter(path -> path.toString().contains("TigerVNC viewer"))
                        .findFirst();
            } catch (IOException e) {
                ErrorEventFactory.fromThrowable(e).handle();
                return Optional.empty();
            }
        }
    }
}
