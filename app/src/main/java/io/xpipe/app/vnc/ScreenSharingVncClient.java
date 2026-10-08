package io.xpipe.app.vnc;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Builder
@Jacksonized
@JsonTypeName("screenSharing")
public class ScreenSharingVncClient implements ExternalApplicationType.MacApplication, ExternalVncClient {

    @Override
    public void launch(VncLaunchConfig configuration) throws Exception {
        var pw = configuration.retrievePassword();
        // Special characters in the credentials would otherwise break the URL
        var credentials = (encodeUserInfo(configuration.retrieveUsername().orElse(""))
                + pw.map(secretValue -> ":" + encodeUserInfo(secretValue.getSecretValue()))
                        .orElse(""));
        var address = configuration.getHost() + ":" + configuration.getPort();
        var args = "vnc://" + (credentials.isEmpty() ? "" : credentials + "@") + address;
        var command = launchCommand(CommandBuilder.of().addLiteral(args), false);
        if (pw.isPresent()) {
            command.sensitive();
        }
        command.execute();
    }

    private static String encodeUserInfo(String s) {
        // URLEncoder uses form encoding, where a space is a +, which is not valid in the user info of a URL
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @Override
    public boolean supportsPasswords() {
        return true;
    }

    @Override
    public String getWebsite() {
        return "https://support.apple.com/en-is/guide/mac-help/mh14066/15.0/mac/15.0";
    }

    @Override
    public String getApplicationName() {
        return "Screen Sharing";
    }
}
