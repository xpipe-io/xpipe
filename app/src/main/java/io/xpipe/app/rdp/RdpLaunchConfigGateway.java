package io.xpipe.app.rdp;

import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.RdpConfig;
import io.xpipe.app.util.SecretValue;

import lombok.Value;

import java.util.Optional;
import java.util.UUID;

@Value
public class RdpLaunchConfigGateway {

    String host;
    String username;
    SecretValue password;
    boolean reused;

    public Optional<String> getDomain() {
        var domain = username.contains("\\") ? username.split("\\\\")[0] : null;
        return Optional.ofNullable(domain);
    }

    public String getUsernameWithoutDomain() {
        if (username.contains("\\")) {
            return username.split("\\\\")[1];
        } else {
            return username;
        }
    }

}
