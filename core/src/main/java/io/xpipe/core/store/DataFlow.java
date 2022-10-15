package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DataFlow {
    @JsonProperty("input")
    INPUT,
    @JsonProperty("output")
    OUTPUT,
    @JsonProperty("inputOutput")
    INPUT_OUTPUT,
    @JsonProperty("transformer")
    TRANSFORMER;

    public boolean hasInput() {
        return this == INPUT || this == INPUT_OUTPUT;
    }

    public boolean hasOutput() {
        return this == OUTPUT || this == INPUT_OUTPUT;
    }
}
