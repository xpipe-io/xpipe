package io.xpipe.app.storage;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum DataStoreColor {
    @JsonProperty("red")
    RED("red", "\uD83D\uDFE5"),

    @JsonProperty("green")
    GREEN("green", "\uD83D\uDFE9"),

    @JsonProperty("yellow")
    YELLOW("yellow", "\uD83D\uDFE8"),

    @JsonProperty("blue")
    BLUE("blue", "\uD83D\uDFE6");

    private final String id;
    private final String emoji;

    DataStoreColor(String id, String emoji) {
        this.id = id;
        this.emoji = emoji;
    }
}
