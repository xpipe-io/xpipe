package io.xpipe.app.rdp;

import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.RdpConfig;
import io.xpipe.app.util.SecretValue;

import lombok.Value;

import java.util.UUID;

@Value
public class RdpLaunchConfig {
    String title;
    DataStoreEntry entry;
    RdpConfig config;
    UUID storeId;
    SecretValue password;

    public boolean isRemoteApp() {
        return config.get("remoteapplicationprogram").isPresent();
    }
}
