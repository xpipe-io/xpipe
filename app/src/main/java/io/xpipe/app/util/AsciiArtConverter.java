package io.xpipe.app.util;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

public class AsciiArtConverter {

    public static String convert(Image img) {
        var builder = new StringBuilder();
        for (int i = 0; i < img.getHeight(); i++) {
            for (int j = 0; j < img.getWidth(); j++) {
                var pixelcolor = img.getPixelReader().getColor(j, i);
                var pixelValue = 255.0 * (((pixelcolor.getRed() * 0.30) + (pixelcolor.getBlue() * 0.59) + (pixelcolor.getGreen() * 0.11)));
                var pixelChar = colorToChar(pixelValue);
                builder.append(pixelChar);
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private static char colorToChar(double v) {
        if (v >= 240) {
            return ' ';
        } else if (v >= 210) {
            return '.';
        } else if (v >= 190) {
            return '*';
        } else if (v >= 170) {
            return '+';
        } else if (v >= 120) {
            return '^';
        } else if (v >= 110) {
            return '$';
        } else if (v >= 80) {
            return '4';
        } else if (v >= 60) {
            return '#';
        } else {
            return ' ';
        }
    }
}
