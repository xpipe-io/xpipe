package io.xpipe.app.vnc;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.core.process.CommandBuilder;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public abstract class RealVncClient implements ExternalVncClient {

    @Override
    public boolean supportsPasswords() {
        return false;
    }

    protected CommandBuilder createBuilder(VncLaunchConfig configuration) throws Exception {
        var builder = CommandBuilder.of()
                .addQuoted(configuration.getHost() + ":" + configuration.getPort())
                .addQuotedKeyValue("-ColorLevel", "full")
                .addQuotedKeyValue("-SecurityNotificationTimeout", "0")
                .addQuotedKeyValue("-WarnUnencrypted", configuration.isTunneled() ? "0" : "1");
        configuration.retrieveUsername().ifPresent(s -> builder.addQuotedKeyValue("-UserName", s));
        return builder;
    }

    @Builder
    @Jacksonized
    @JsonTypeName("realVnc")
    public static class Windows extends RealVncClient implements ExternalApplicationType.WindowsType {

        @Override
        public boolean detach() {
            return true;
        }

        @Override
        public Optional<Path> determineFromPath() {
            var found = WindowsType.super.determineFromPath();
            return found.filter(path -> path.toString().contains("RealVNC"));
        }

        @Override
        public String getExecutable() {
            return "vncviewer.exe";
        }

        @Override
        public Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("PROGRAMFILES"))
                            .resolve("RealVNC")
                            .resolve("VNC Viewer")
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
    @JsonTypeName("realVnc")
    public static class Linux extends RealVncClient implements ExternalApplicationType.PathApplication {

        @Override
        public String getExecutable() {
            return "vncviewer";
        }

        @Override
        public boolean detach() {
            return true;
        }

        @Override
        public void launch(VncLaunchConfig configuration) throws Exception {
            var builder = createBuilder(configuration);
            launch(builder);
        }
    }


    @Builder
    @Jacksonized
    @JsonTypeName("realVnc")
    public static class MacOs extends RealVncClient implements ExternalApplicationType.MacApplication {

        @Override
        public void launch(VncLaunchConfig configuration) throws Exception {
            var builder = createBuilder(configuration);
            launchCommand(builder, true).execute();
        }

        @Override
        public String getApplicationName() {
            return "VNC Viewer";
        }
    }
}
