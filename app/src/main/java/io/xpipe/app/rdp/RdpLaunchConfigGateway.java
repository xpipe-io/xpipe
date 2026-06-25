package io.xpipe.app.rdp;

import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.RdpConfig;
import io.xpipe.app.util.SecretValue;

import lombok.Value;

import java.util.UUID;

@Value
public class RdpLaunchConfigGateway {

    String host;
    String username;
    SecretValue password;
    boolean reused;
}
