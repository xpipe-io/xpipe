package io.xpipe.app.vnc;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.core.AppLocalTemp;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.spice.ExternalSpiceClient;
import io.xpipe.app.spice.SpiceLaunchConfig;
import io.xpipe.app.util.RdpConfig;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public abstract class RemoteViewerVncClient implements ExternalVncClient {

    protected CommandBuilder createBuilder(VncLaunchConfig configuration) throws Exception {
        var vv = """
                 [virt-viewer]
                 type=vnc
                 host=%s
                 port=%s
                 title=%s
                 """.formatted(configuration.getHost(), configuration.getPort(), configuration.getTitle());

        var user = configuration.retrieveUsername();
        if (user.isPresent()) {
            vv += "username=" + user.get() + "\n";
        }

        var pass = configuration.retrievePassword();
        if (pass.isPresent()) {
            vv += "password=" + pass.get().getSecretValue() + "\n";
        }

        var file = writeVncConfigFile(configuration.getTitle(), vv);
        var builder = CommandBuilder.of().addFile(file);
        return builder;
    }

    private Path writeVncConfigFile(String title, String content) throws Exception {
        var name = OsFileSystem.ofLocal().makeFileSystemCompatible(title);
        var file = AppLocalTemp.getLocalTempDataDirectory("vnc").resolve(name + ".vv");
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
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
        public void launch(VncLaunchConfig configuration) throws Exception {
            var builder = createBuilder(configuration);
            launch(builder);
        }
    }

    @Builder
    @Jacksonized
    @JsonTypeName("remoteViewer")
    public static class Linux extends RemoteViewerVncClient implements ExternalApplicationType.LinuxApplication {

        @Override
        public void launch(VncLaunchConfig configuration) throws Exception {
            var builder = createBuilder(configuration);
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
        public void launch(VncLaunchConfig configuration) throws Exception {
            var builder = createBuilder(configuration);
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
