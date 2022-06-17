package io.xpipe.core.charsetter;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public enum NewLine {

    @JsonProperty("lf")
    LF("\n", "lf"),
    @JsonProperty("crlf")
    CRLF("\r\n", "crlf");

    public static NewLine platform() {
        return Arrays.stream(values())
                .filter(n -> n.getNewLine().equals(System.getProperty("line.separator")))
                .findFirst().orElseThrow();
    }

    public static NewLine id(String id) {
        return Arrays.stream(values())
                .filter(n -> n.getId().equalsIgnoreCase(id))
                .findFirst().orElseThrow();
    }

    private final String newLine;
    private final String id;

    NewLine(String newLine, String id) {
        this.newLine = newLine;
        this.id = id;
    }

    public String getNewLine() {
        return newLine;
    }

    public String getId() {
        return id;
    }
}
