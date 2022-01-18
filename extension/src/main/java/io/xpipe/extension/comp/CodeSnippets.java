package io.xpipe.extension.comp;

import javafx.scene.paint.Color;

public class CodeSnippets {

    public static final ColorScheme LIGHT_MODE = new ColorScheme(
            Color.valueOf("0033B3"),
            Color.valueOf("000000"),
            Color.valueOf("000000"),
            Color.valueOf("067D17")
    );

    public static record ColorScheme(
            Color keyword, Color identifier, Color type, Color string) {

    }
}
