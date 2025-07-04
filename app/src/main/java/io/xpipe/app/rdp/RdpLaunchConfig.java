package io.xpipe.app.rdp;

import io.xpipe.app.util.RdpConfig;
import io.xpipe.core.SecretValue;

import lombok.Value;

import java.util.UUID;

@Value
public class RdpLaunchConfig {
    String title;
    RdpConfig config;
    UUID storeId;
    SecretValue password;
}
