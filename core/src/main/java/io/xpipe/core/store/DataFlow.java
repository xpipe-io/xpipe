package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DataFlow {
    @JsonProperty("input")
    INPUT("Input"),
    @JsonProperty("output")
    OUTPUT("Output"),
    @JsonProperty("inputOrOutput")
    INPUT_OR_OUTPUT("Input or Output"),
    @JsonProperty("inputOutput")
    INPUT_OUTPUT("Input/Output"),
    @JsonProperty("transformer")
    TRANSFORMER("Transformer");

    private final String displayName;

    DataFlow(String displayName) {
        this.displayName = displayName;
    }

    public boolean hasInput() {
        return this == INPUT || this == INPUT_OUTPUT;
    }

    public boolean hasOutput() {
        return this == OUTPUT || this == INPUT_OUTPUT;
    }

    public String getDisplayName() {
        return displayName;
    }
}
