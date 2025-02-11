package io.xpipe.app.util;

import javafx.scene.paint.Color;

public class ColorHelper {

    public static String toWeb(Color c) {
        var hex = String.format(
                "#%02X%02X%02X%02X",
                (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255), (int)
                        (c.getOpacity() * 255));
        return hex;
    }

    public static Color withOpacity(Color c, double opacity) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), opacity);
    }
}
