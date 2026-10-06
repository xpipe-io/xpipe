package io.xpipe.app.vnc;

import io.xpipe.app.core.AppLocalTemp;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.util.GlobalTimer;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public abstract class RemoteViewerVncClient implements ExternalVncClient {

    private static String escapeValue(String value) {
        // Values are read with GLib key files
        return value.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    protected abstract void launchApplication(CommandBuilder builder) throws Exception;

    @Override
    public void launch(VncLaunchConfig configuration) throws Exception {
        var file = writeVncConfigFile(configuration);
        launchApplication(CommandBuilder.of().addFile(file));
    }

    private Path writeVncConfigFile(VncLaunchConfig configuration) throws Exception {
        var vv = new StringBuilder();
        vv.append("[virt-viewer]\n");
        vv.append("type=vnc\n");
        vv.append("host=").append(escapeValue(configuration.getHost())).append("\n");
        vv.append("port=").append(configuration.getPort()).append("\n");
        vv.append("title=").append(escapeValue(configuration.getTitle())).append("\n");
        vv.append("delete-this-file=1\n");

        var user = configuration.retrieveUsername();
        if (user.isPresent()) {
            vv.append("username=").append(escapeValue(user.get())).append("\n");
        }

        var pass = configuration.retrievePassword();
        if (pass.isPresent()) {
            vv.append("password=")
                    .append(escapeValue(pass.get().getSecretValue()))
                    .append("\n");
        }

        var name = OsFileSystem.ofLocal().makeFileSystemCompatible(configuration.getTitle());
        var file = AppLocalTemp.getLocalTempDataDirectory("spice").resolve(name + ".vv");
        Files.writeString(file, vv.toString());
        return file;
    }

    @Override
    public String getWebsite() {
        return "https://virt-manager.org";
    }

    @Override
    public boolean supportsPasswords() {
        return true;
    }

    @Builder
    @Jacksonized
    @JsonTypeName("remoteViewer")
    public static class Windows extends RemoteViewerVncClient implements ExternalApplicationType.WindowsType {

        @Override
        public boolean detach() {
            return false;
        }

        @Override
        public String getExecutable() {
            return "remote-viewer.exe";
        }

        @Override
        public Optional<Path> determineInstallation() {
            try (var stream = Files.list(AppSystemInfo.ofWindows().getProgramFiles())) {
                var l = stream.toList();
                var found = l.stream()
                        .filter(path -> path.toString().contains("VirtViewer"))
                        .findFirst();
                if (found.isEmpty()) {
                    return Optional.empty();
                }

                return Optional.ofNullable(found.get().resolve("bin", "remote-viewer.exe"));
            } catch (IOException e) {
                ErrorEventFactory.fromThrowable(e).handle();
                return Optional.empty();
            }
        }

        @Override
        protected void launchApplication(CommandBuilder builder) throws Exception {
            launch(builder);
        }
    }

    @Builder
    @Jacksonized
    @JsonTypeName("remoteViewer")
    public static class Linux extends RemoteViewerVncClient implements ExternalApplicationType.LinuxApplication {

        @Override
        protected void launchApplication(CommandBuilder builder) throws Exception {
            launch(builder);
        }

        @Override
        public String getExecutable() {
            return "remote-viewer";
        }

        @Override
        public boolean detach() {
            return true;
        }

        @Override
        public String getFlatpakId() {
            return "org.virt_manager.virt-viewer";
        }
    }

    @Builder
    @Jacksonized
    @JsonTypeName("remoteViewer")
    public static class MacOs extends RemoteViewerVncClient implements ExternalApplicationType.PathApplication {

        @Override
        protected void launchApplication(CommandBuilder builder) throws Exception {
            launch(builder);
        }

        @Override
        public String getExecutable() {
            return "remote-viewer";
        }

        @Override
        public boolean detach() {
            return true;
        }
    }
}
