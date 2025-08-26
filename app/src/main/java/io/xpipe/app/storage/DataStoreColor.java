package io.xpipe.app.storage;

import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;

@Getter
public enum DataStoreColor {
    @JsonProperty("red")
    RED("red", "\uD83D\uDD34", Color.DARKRED),

    @JsonProperty("yellow")
    YELLOW("yellow", "\uD83D\uDFE1", Color.web("#999922")),

    @JsonProperty("green")
    GREEN("green", "\uD83D\uDFE2", Color.DARKGREEN),

    @JsonProperty("cyan")
    CYAN("cyan", "\uD83D\uDFE9", Color.CYAN),

    @JsonProperty("blue")
    BLUE("blue", "\uD83D\uDD35", Color.DARKBLUE),

    @JsonProperty("purple")
    VIOLET("purple", "\uD83D\uDFE3", Color.VIOLET);

    private final String id;
    private final String emoji;
    private final Color terminalColor;

    DataStoreColor(String id, String emoji, Color terminalColor) {
        this.id = id;
        this.emoji = emoji;
        this.terminalColor = terminalColor;
    }

    public static Region createDisplayGraphic(DataStoreColor color) {
        var b = new Rectangle(8, 8);
        b.setArcWidth(4);
        b.setArcHeight(4);
        b.getStyleClass().add("dot");
        b.getStyleClass().add("color-box");
        b.getStyleClass().add(color != null ? color.getId() : "gray");

        var d = new Rectangle(10, 10);
        d.setArcWidth(4 + 2);
        d.setArcHeight(4 + 2);
        d.getStyleClass().add("dot");
        d.getStyleClass().add("color-box");
        d.getStyleClass().add(color != null ? color.getId() : "gray");

        var s = new StackPane(d, b);
        return s;
    }

    public static void applyStyleClasses(DataStoreColor color, Node node) {
        var newList = new ArrayList<>(node.getStyleClass());
        newList.removeIf(s -> Arrays.stream(DataStoreColor.values())
                .anyMatch(dataStoreColor -> dataStoreColor.getId().equals(s)));
        newList.remove("gray");
        if (color != null) {
            newList.add(color.getId());
        } else {
            newList.add("gray");
        }
        node.getStyleClass().setAll(newList);
    }

    private String format(double val) {
        String in = Integer.toHexString((int) Math.round(val * 255));
        return in.length() == 1 ? "0" + in : in;
    }

    public String toHexString() {
        var value = terminalColor;
        return "#" + (format(value.getRed()) + format(value.getGreen()) + format(value.getBlue())).toUpperCase();
    }
}
