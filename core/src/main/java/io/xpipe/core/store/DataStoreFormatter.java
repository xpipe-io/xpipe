package io.xpipe.core.store;

public class DataStoreFormatter {

    public static String ellipsis(String input, int length) {
        var end = Math.min(input.length(), length);
        if (end < input.length()) {
            return input.substring(0, end) + "...";
        }
        return input;
    }

    public static String specialFormatHostName(String input) {
        if (input.contains(":")) {
            input = input.split(":")[0];
        }

        if (input.endsWith(".rds.amazonaws.com")) {
            var split = input.split("\\.");
            var name = split[0];
            var region = split[2];
            return String.format("RDS %s @ %s", name, region);
        }
        return null;
    }
}
