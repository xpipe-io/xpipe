package io.xpipe.app.spice;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.vnc.ExternalVncClient;
import io.xpipe.app.vnc.VncLaunchConfig;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public abstract class VirtViewerSpiceClient implements ExternalSpiceClient {

    protected CommandBuilder createBuilder(SpiceLaunchConfig configuration) {
        var builder = CommandBuilder.of().addFile(configuration.getFile());
        return builder;
    }

    @Override
    public String getWebsite() {
        return "https://virt-manager.org";
    }

    @Builder
    @Jacksonized
    @JsonTypeName("virtViewer")
    public static class Windows extends VirtViewerSpiceClient implements ExternalApplicationType.WindowsType {

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
                var found = l.stream().filter(path -> path.toString().contains("VirtViewer")).findFirst();
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
        public void launch(SpiceLaunchConfig configuration) throws Exception {
            var builder = createBuilder(configuration);
            launch(builder);
        }
    }

    @Builder
    @Jacksonized
    @JsonTypeName("virtViewer")
    public static class Linux extends VirtViewerSpiceClient implements ExternalApplicationType.LinuxApplication {

        @Override
        public void launch(SpiceLaunchConfig configuration) throws Exception {
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
    @JsonTypeName("virtViewer")
    public static class MacOs extends VirtViewerSpiceClient implements ExternalApplicationType.PathApplication {

        @Override
        public void launch(SpiceLaunchConfig configuration) throws Exception {
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
