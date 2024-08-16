package io.xpipe.core.process;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum ShellTtyState {
    @JsonProperty("none")
    NONE(true, false, false, true, true),
    @JsonProperty("merged")
    MERGED_STDERR(false, true, false, false, false),
    @JsonProperty("pty")
    PTY_ALLOCATED(false, true, true, false, false);

    private final boolean hasSeparateStreams;
    private final boolean hasAnsiEscapes;
    private final boolean echoesAllInput;
    private final boolean supportsInput;
    private final boolean preservesOutput;

    ShellTtyState(
            boolean hasSeparateStreams,
            boolean hasAnsiEscapes,
            boolean echoesAllInput,
            boolean supportsInput,
            boolean preservesOutput) {
        this.hasSeparateStreams = hasSeparateStreams;
        this.hasAnsiEscapes = hasAnsiEscapes;
        this.echoesAllInput = echoesAllInput;
        this.supportsInput = supportsInput;
        this.preservesOutput = preservesOutput;
    }
}
