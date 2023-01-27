package io.xpipe.cli.util;

import picocli.CommandLine;

public record KeyValue(String key, String value) {

    public KeyValue normalize() {
        return new KeyValue(key.toLowerCase(), value.toLowerCase());
    }

    public static class Converter implements CommandLine.ITypeConverter<KeyValue> {

        @Override
        public KeyValue convert(String value) throws Exception {
            var split = value.split("=");
            if (split.length != 2) {
                throw new IllegalArgumentException("Not a key value pair: " + value);
            }

            var key = split[0].replace('"', ' ');
            var val = split[1].replace('"', ' ');
            return new KeyValue(key, val).normalize();
        }
    }
}
