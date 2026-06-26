package io.xpipe.app.rdp;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.*;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("remmina")
@Value
@Jacksonized
@Builder
public class RemminaRdpClient implements ExternalApplicationType.LinuxApplication, ExternalRdpClient {

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        // Remmina does not support RemoteApps
        if (configuration.isRemoteApp()) {
            var freerdp = new FreeRdpClient();
            if (freerdp.isAvailable()) {
                freerdp.launch(configuration);
                return;
            }
        }

        var file = RemminaHelper.writeRemminaRdpConfigFile(configuration);
        LocalFileTracker.deleteOnExit(file);
        launch(CommandBuilder.of().add("-c").addFile(file.toString()));
    }

    @Override
    public boolean supportsPasswordPassing() {
        return true;
    }

    @Override
    public String getWebsite() {
        return "https://remmina.org/";
    }

    @Override
    public String getExecutable() {
        return "remmina";
    }

    @Override
    public boolean detach() {
        return true;
    }

    @Override
    public String getFlatpakId() {
        return "org.remmina.Remmina";
    }
}
