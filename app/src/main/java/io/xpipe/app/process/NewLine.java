package io.xpipe.app.process;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public enum NewLine {
    @JsonProperty("lf")
    LF("\n", "lf"),
    @JsonProperty("crlf")
    CRLF("\r\n", "crlf");

    private final String newLine;

    @Getter
    private final String id;

    NewLine(String newLine, String id) {
        this.newLine = newLine;
        this.id = id;
    }

    public String getNewLineString() {
        return newLine;
    }
}
