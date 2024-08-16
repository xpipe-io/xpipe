package io.xpipe.app.ext;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DataStoreUsageCategory {
    @JsonProperty("shell")
    SHELL,
    @JsonProperty("tunnel")
    TUNNEL,
    @JsonProperty("script")
    SCRIPT,
    @JsonProperty("database")
    DATABASE,
    @JsonProperty("command")
    COMMAND,
    @JsonProperty("desktop")
    DESKTOP,
    @JsonProperty("group")
    GROUP,
    @JsonProperty("serial")
    SERIAL
}
