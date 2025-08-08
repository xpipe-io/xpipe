package io.xpipe.app.vnc;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@JsonTypeName("screenSharing")
public class ScreenSharingVncClient implements ExternalApplicationType.MacApplication, ExternalVncClient {

    @Override
    public String getWebsite() {
        return "https://support.apple.com/en-is/guide/mac-help/mh14066/15.0/mac/15.0";
    }

    @Override
    public boolean supportsPasswords() {
        return true;
    }

    @Override
    public void launch(VncLaunchConfig configuration) throws Exception {
        var pw = configuration.retrievePassword();
        var credentials = (configuration.retrieveUsername().orElse("")
                + pw.map(secretValue -> ":" + secretValue.getSecretValue()).orElse(""));
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
