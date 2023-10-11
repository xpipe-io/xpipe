package io.xpipe.app.fxcomps.impl;

import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.geometry.Point2D;

import java.util.regex.Pattern;

public class SvgHelper {

    public static Size parseSize(String string) {
        for (SizeUnits unit : SizeUnits.values()) {
            if (string.endsWith(unit.toString())) {
                return new Size(
                        Double.parseDouble(string.substring(
                                0, string.length() - unit.toString().length())),
                        unit);
            }
        }
        return new Size(Double.parseDouble(string), SizeUnits.PX);
    }

    public static Point2D getDimensions(String val) {
        var regularExpression = Pattern.compile("<svg[^>]+?width=\"([^ ]+)\"", Pattern.DOTALL);
        var matcher = regularExpression.matcher(val);

        if (!matcher.find()) {
            var viewBox = Pattern.compile(
                    "<svg.+?viewBox=\"([\\d.]+)\\s+([\\d.]+)\\s+([\\d.]+)\\s+([\\d.]+)\"", Pattern.DOTALL);
            matcher = viewBox.matcher(val);
            if (matcher.find()) {
                return new Point2D(
                        parseSize(matcher.group(3)).pixels(),
                        parseSize(matcher.group(4)).pixels());
            }
        }

        var width = matcher.group(1);
        regularExpression = Pattern.compile("<svg.+?height=\"([^ ]+)\"", Pattern.DOTALL);
        matcher = regularExpression.matcher(val);
        matcher.find();
        var height = matcher.group(1);
        return new Point2D(parseSize(width).pixels(), parseSize(height).pixels());
    }
}
