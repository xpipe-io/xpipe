package io.xpipe.app.rdp;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.WindowsRegistry;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.io.FileUtils;

import java.nio.file.Path;
import java.util.Optional;

@JsonTypeName("devolutions")
@Value
@Jacksonized
@Builder
public class DevolutionsRdpClient implements ExternalApplicationType.WindowsType, ExternalRdpClient {

    @Override
    public boolean detach() {
        return true;
    }

    @Override
    public String getExecutable() {
        return "RemoteDesktopManager";
    }

    @Override
    public Optional<Path> determineInstallation() {
        try {
            var r = WindowsRegistry.local()
                    .readStringValueIfPresent(
                            WindowsRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Classes\\rdm\\DefaultIcon");
            return r.map(Path::of);
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).omit().handle();
            return Optional.empty();
        }
    }

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        var config = writeRdpConfigFile(configuration.getTitle(), configuration.getConfig());
        launch(CommandBuilder.of().addFile(config));
        ThreadHelper.runFailableAsync(() -> {
            // Startup is slow
            ThreadHelper.sleep(10000);
            FileUtils.deleteQuietly(config.toFile());
        });
    }

    @Override
    public boolean supportsPasswordPassing() {
        return false;
    }

    @Override
    public String getWebsite() {
        return "https://devolutions.net/remote-desktop-manager/";
    }

    @Override
    public String getId() {
        return "app.devolutions";
    }
}
