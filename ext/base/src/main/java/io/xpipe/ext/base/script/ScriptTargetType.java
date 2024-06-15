package io.xpipe.ext.base.script;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ScriptTargetType {
    @JsonProperty("shellInit")
    SHELL_INIT,
    @JsonProperty("shellSession")
    SHELL_SESSION,
    @JsonProperty("file")
    FILE
}
