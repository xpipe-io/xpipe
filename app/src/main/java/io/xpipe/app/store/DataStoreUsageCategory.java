package io.xpipe.app.store;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DataStoreUsageCategory {
    @JsonProperty("shell")
    SHELL,
    @JsonProperty("tunnel")
    TUNNEL,
    @JsonProperty("script")
    SCRIPT,
    @JsonProperty("command")
    COMMAND,
    @JsonProperty("desktop")
    DESKTOP,
    @JsonProperty("group")
    GROUP,
    @JsonProperty("serial")
    SERIAL,
    @JsonProperty("identity")
    IDENTITY,
    @JsonProperty("fileSystem")
    FILE_SYSTEM
}
