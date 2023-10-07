package io.xpipe.app.storage;

import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.scene.paint.Color;
import lombok.Getter;

@Getter
public enum DataStoreColor {
    @JsonProperty("red")
    RED("red", "\uD83D\uDFE5", Color.BLUE),

    @JsonProperty("green")
    GREEN("green", "\uD83D\uDFE9", Color.BLUE),

    @JsonProperty("yellow")
    YELLOW("yellow", "\uD83D\uDFE8", Color.BLUE),

    @JsonProperty("blue")
    BLUE("blue", "\uD83D\uDD35", Color.BLUE);

    private final String id;
    private final String emoji;
    private final Color terminalColor;

    DataStoreColor(String id, String emoji, Color terminalColor) {
        this.id = id;
        this.emoji = emoji;
        this.terminalColor = terminalColor;
    }

    private String format(double val) {
        String in = Integer.toHexString((int) Math.round(val * 255));
        return in.length() == 1 ? "0" + in : in;
    }

    public String toHexString() {
        var value = terminalColor;
        return "#" + (format(value.getRed()) + format(value.getGreen()) + format(value.getBlue()))
                .toUpperCase();
    }
}
