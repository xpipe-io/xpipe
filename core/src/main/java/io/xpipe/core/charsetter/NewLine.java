package io.xpipe.core.charsetter;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Arrays;

public enum NewLine {
    @JsonProperty("lf") LF("\n", "lf"),
    @JsonProperty("crlf") CRLF("\r\n", "crlf");

    private final String newLine;
    @Getter
    private final String id;

    NewLine(String newLine, String id) {
        this.newLine = newLine;
        this.id = id;
    }

    public static NewLine platform() {
        return Arrays.stream(values()).filter(n -> n.getNewLineString().equals(System.getProperty("line.separator"))).findFirst().orElseThrow();
    }

    public static NewLine byId(String id) {
        return Arrays.stream(values()).filter(n -> n.getId().equalsIgnoreCase(id)).findFirst().orElseThrow();
    }

    public String getNewLineString() {
        return newLine;
    }

}
