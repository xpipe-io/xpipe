package io.xpipe.app.rdp;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.LocalFileTracker;
import io.xpipe.app.util.RdpConfig;
import io.xpipe.app.util.RemminaHelper;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.HashSet;
import java.util.List;

@JsonTypeName("krdc")
@Value
@Jacksonized
@Builder
public class KrdcRdpClient implements ExternalApplicationType.LinuxApplication, ExternalRdpClient {

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        // Krdc does not support RemoteApps
        if (configuration.isRemoteApp()) {
            var freerdp = new FreeRdpClient();
            if (freerdp.isAvailable()) {
                freerdp.launch(configuration);
                return;
            }
        }

        RdpConfig c = configuration.getConfig();
        var file = writeRdpConfigFile(configuration.getTitle(), c);
        launch(CommandBuilder.of().addFile(file.toString()));
        LocalFileTracker.deleteOnExit(file);
    }

    @Override
    public boolean supportsPasswordPassing(RdpLaunchConfig config) {
        return false;
    }

    @Override
    public String getWebsite() {
        return "https://apps.kde.org/krdc/";
    }

    @Override
    public String getExecutable() {
        return "krdc";
    }

    @Override
    public boolean detach() {
        return true;
    }

    @Override
    public String getFlatpakId() {
        return "org.kde.krdc";
    }
}
