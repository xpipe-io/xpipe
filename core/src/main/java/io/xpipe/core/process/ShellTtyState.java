package io.xpipe.core.process;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum ShellTtyState {

    @JsonProperty("none")
    NONE(true, false, false),
    @JsonProperty("merged")
    MERGED_STDERR(false, false, false),
    @JsonProperty("pty")
    PTY_ALLOCATED(false, true, true);

    private final boolean hasSeparateStreams;
    private final boolean hasAnsiEscapes;
    private final boolean echoesAllInput;

    ShellTtyState(boolean hasSeparateStreams, boolean hasAnsiEscapes, boolean echoesAllInput) {
        this.hasSeparateStreams = hasSeparateStreams;
        this.hasAnsiEscapes = hasAnsiEscapes;
        this.echoesAllInput = echoesAllInput;
    }
}
