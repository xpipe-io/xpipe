package io.xpipe.app.util;

import io.xpipe.app.storage.DataStoreEntry;

import java.util.Arrays;

public class DataStoreFormatter {

    public static String join(String... elements) {
        return String.join(" ", Arrays.stream(elements).filter(s -> s != null).toList());
    }

    public static String capitalize(String name) {
        if (name == null) {
            return null;
        }

        if (name.isEmpty()) {
            return name;
        }

        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    public static String toApostropheName(DataStoreEntry input) {
        return toName(input, Integer.MAX_VALUE) + "'s";
    }

    public static String toName(DataStoreEntry input, int length) {
        if (input == null) {
            return "?";
        }

        return cut(input.getName(), length);
    }

    public static String split(String left, String separator, String right, int length) {
        var half = (length / 2) - separator.length();
        return cut(left, half) + separator + cut(right, length - half);
    }

    public static String cut(String input, int length) {
        if (input == null) {
            return "";
        }

        var end = Math.min(input.length(), length);
        if (end < input.length()) {
            return input.substring(0, end) + "...";
        }
        return input;
    }

    public static String formatHostName(String input, int length) {
        if (input == null) {
            return null;
        }

        // Remove port
        if (input.contains(":")) {
            input = input.split(":")[0];
        }

        // Check for amazon web services
        if (input.endsWith(".rds.amazonaws.com")) {
            var split = input.split("\\.");
            var name = split[0];
            var region = split[2];
            var lengthShare = (length - 3) / 2;
            return String.format(
                    "%s.%s",
                    DataStoreFormatter.cut(name, lengthShare), DataStoreFormatter.cut(region, length - lengthShare));
        }

        if (input.endsWith(".compute.amazonaws.com") || input.endsWith(".compute.internal")) {
            var split = input.split("\\.");
            var name = split[0];
            var region = split[1];
            var lengthShare = (length - 3) / 2;
            return String.format(
                    "%s.%s",
                    DataStoreFormatter.cut(name, lengthShare), DataStoreFormatter.cut(region, length - lengthShare));
        }

        return cut(input, length);
    }
}
