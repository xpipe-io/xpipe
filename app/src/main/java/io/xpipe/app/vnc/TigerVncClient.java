package io.xpipe.app.vnc;

import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

public abstract class TigerVncClient implements ExternalVncClient {

    protected CommandBuilder createBuilder(VncLaunchConfig configuration) {
        var builder = CommandBuilder.of().addQuoted(configuration.getHost() + ":" + configuration.getPort());
        builder.addQuotedKeyValue("-ReconnectOnError", "off");
        return builder;
    }

    protected void addCredentialsEnvironment(CommandBuilder builder, VncLaunchConfig configuration) throws Exception {
        var username = configuration.retrieveUsername();
        if (username.isPresent()) {
            builder.fixedEnvironment("VNC_USERNAME", username.get());
        }

        if (configuration.hasFixedPassword()) {
            var pw = configuration.retrievePassword();
            if (pw.isPresent()) {
                builder.fixedEnvironment("VNC_PASSWORD", pw.get().getSecretValue());
                builder.sensitive();
            }
        }
    }

    @Override
    public String getWebsite() {
        return "https://tigervnc.org/";
    }

    @Builder
    @Jacksonized
    @JsonTypeName("tigerVnc")
    public static class Windows extends TigerVncClient implements ExternalApplicationType.WindowsType {

        @Override
        public boolean detach() {
            return false;
        }

        @Override
        public String getExecutable() {
            return "vncviewer.exe";
        }

        @Override
        public Optional<Path> determineInstallation() {
            return Optional.of(AppSystemInfo.ofWindows()
                            .getProgramFiles()
                            .resolve("TigerVNC")
                            .resolve("vncviewer.exe"))
                    .filter(path -> Files.exists(path));
        }

        @Override
        public Optional<Path> determineFromPath() {
            var found = WindowsType.super.determineFromPath();
            return found.filter(path -> path.toString().contains("TigerVNC"));
        }

        @Override
        public void launch(VncLaunchConfig configuration) throws Exception {
            var builder = createBuilder(configuration);
            addCredentialsEnvironment(builder, configuration);
            launch(builder);
        }

        @Override
        public boolean supportsPasswords() {
            return true;
        }
    }

    @Builder
    @Jacksonized
    @JsonTypeName("tigerVnc")
    public static class Linux extends TigerVncClient implements ExternalApplicationType.LinuxApplication {

        @Override
        public void launch(VncLaunchConfig configuration) throws Exception {
            var builder = createBuilder(configuration);
            addCredentialsEnvironment(builder, configuration);
            launch(builder);
        }

        @Override
        public boolean supportsPasswords() {
            return true;
        }

        @Override
        public String getExecutable() {
            return "xtigervncviewer";
        }

        @Override
        public boolean detach() {
            return true;
        }

        @Override
        public String getFlatpakId() {
            return "org.tigervnc.vncviewer";
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
            addCredentialsEnvironment(builder, configuration);
            var open = CommandBuilder.of().add("open", "-n", "-a").addFile(loc).add("--args");
            builder.add(0, open);
            LocalShell.getShell().command(builder).execute();
        }

        @Override
        public boolean supportsPasswords() {
            return true;
        }

        @Override
        public String getExecutable() {
            return "VNCViewer";
        }

        private static FileTime getModifiedTime(Path app) {
            try {
                return Files.getLastModifiedTime(app);
            } catch (IOException e) {
                return FileTime.fromMillis(0);
            }
        }

        @Override
        public Optional<Path> determineInstallation() {
            try (var appsStream = Files.list(Path.of("/Applications"))) {
                var dirs = appsStream.toList();
                return dirs.stream()
                        // The app is named like "TigerVNC Viewer 1.15.0.app"
                        .filter(path -> path.getFileName()
                                .toString()
                                .toLowerCase(Locale.ROOT)
                                .startsWith("tigervnc viewer"))
                        // Prefer the most recently modified one if multiple versions are installed
                        .max(Comparator.comparing(MacOs::getModifiedTime));
            } catch (IOException e) {
                ErrorEventFactory.fromThrowable(e).handle();
                return Optional.empty();
            }
        }
    }
}
