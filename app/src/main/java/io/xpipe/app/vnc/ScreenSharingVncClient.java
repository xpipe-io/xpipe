package io.xpipe.app.vnc;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.util.SecretValue;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Builder
@Jacksonized
@JsonTypeName("screenSharing")
public class ScreenSharingVncClient implements ExternalApplicationType.MacApplication, ExternalVncClient {

    @Override
    public boolean supportsPasswords() {
        return true;
    }

    @Override
    public void launch(VncLaunchConfig configuration) throws Exception {
        var pw = configuration.retrievePassword();
        var credentials = (configuration.retrieveUsername().orElse("") + pw.map(secretValue -> ":" + secretValue.getSecretValue()).orElse(""));
        var address = configuration.getHost() + ":" + configuration.getPort();
        var args = "vnc://" + credentials + "@" + address;
        var command = launchCommand(CommandBuilder.of().add(args), false);
        if (pw.isPresent()) {
            command.sensitive();
        }
        command.execute();
    }

    @Override
    public String getApplicationName() {
        return "Screen Sharing";
    }
}
