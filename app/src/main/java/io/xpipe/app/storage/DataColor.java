package io.xpipe.app.storage;

import javafx.scene.Node;
import javafx.scene.paint.Color;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;

@Getter
public enum DataColor {
    @JsonProperty("red")
    RED("red", "\uD83D\uDD34", Color.DARKRED),

    @JsonProperty("green")
    GREEN("green", "\uD83D\uDFE2", Color.DARKGREEN),

    @JsonProperty("yellow")
    YELLOW("yellow", "\uD83D\uDFE1", Color.web("#999922")),

    @JsonProperty("blue")
    BLUE("blue", "\uD83D\uDD35", Color.DARKBLUE);

    private final String id;
    private final String emoji;
    private final Color terminalColor;

    DataColor(String id, String emoji, Color terminalColor) {
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
        return "#" + (format(value.getRed()) + format(value.getGreen()) + format(value.getBlue())).toUpperCase();
    }

    public static void applyStyleClasses(DataColor color, Node node) {
        var newList = new ArrayList<>(node.getStyleClass());
        newList.removeIf(s -> Arrays.stream(DataColor.values())
                .anyMatch(dataStoreColor -> dataStoreColor.getId().equals(s)));
        newList.remove("gray");
        if (color != null) {
            newList.add(color.getId());
        } else {
            newList.add("gray");
        }
        node.getStyleClass().setAll(newList);
    }
}
