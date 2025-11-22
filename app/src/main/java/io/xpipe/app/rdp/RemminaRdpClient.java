package io.xpipe.app.rdp;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.*;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.*;

@JsonTypeName("remmina")
@Value
@Jacksonized
@Builder
public class RemminaRdpClient implements ExternalApplicationType.LinuxApplication, ExternalRdpClient {

    private List<String> toStrip() {
        return List.of("auto connect", "password 51", "prompt for credentials", "smart sizing");
    }

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        RdpConfig c = configuration.getConfig();

        // Remmina does not support RemoteApps
        if (c.get("remoteapplicationprogram").isPresent()) {
            var freerdp = new FreeRdpClient();
            if (freerdp.isAvailable()) {
                freerdp.launch(configuration);
                return;
            }
        }

        var l = new HashSet<>(c.getContent().keySet());
        toStrip().forEach(l::remove);
        if (l.size() == 2 && l.contains("username") && l.contains("full address")) {
            var encrypted = RemminaHelper.encryptPassword(configuration.getPassword());
            if (encrypted.isPresent()) {
                var file = RemminaHelper.writeRemminaRdpConfigFile(configuration, encrypted.get());
                launch(CommandBuilder.of().add("-c").addFile(file.toString()));
                return;
            }
        }

        var file = writeRdpConfigFile(configuration.getTitle(), c);
        launch(CommandBuilder.of().add("-c").addFile(file.toString()));
        LocalFileTracker.deleteOnExit(file);
    }

    @Override
    public boolean supportsPasswordPassing() {
        return false;
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
