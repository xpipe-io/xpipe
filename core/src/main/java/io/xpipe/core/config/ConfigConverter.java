package io.xpipe.core.config;

import java.nio.charset.Charset;

public abstract class ConfigConverter<T> {

    public static final ConfigConverter<Charset> CHARSET = new ConfigConverter<Charset>() {
        @Override
        protected Charset fromString(String s) {
            return Charset.forName(s);
        }

        @Override
        protected String toString(Charset value) {
            return value.name();
        }
    };

    public static final ConfigConverter<String> STRING = new ConfigConverter<String>() {
        @Override
        protected String fromString(String s) {
            return s;
        }

        @Override
        protected String toString(String value) {
            return value;
        }
    };

    public static final ConfigConverter<Character> CHARACTER = new ConfigConverter<Character>() {
        @Override
        protected Character fromString(String s) {
            if (s.length() != 1) {
                throw new IllegalArgumentException("Invalid character: " + s);
            }

            return s.toCharArray()[0];
        }

        @Override
        protected String toString(Character value) {
            return value.toString();
        }
    };

    public static final ConfigConverter<Boolean> BOOLEAN = new ConfigConverter<Boolean>() {
        @Override
        protected Boolean fromString(String s) {
            if (s.equalsIgnoreCase("y") || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
                return true;
            }

            if (s.equalsIgnoreCase("n") || s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")) {
                return false;
            }

            throw new IllegalArgumentException("Invalid boolean: " + s);
        }

        @Override
        protected String toString(Boolean value) {
            return value ? "yes" : "no";
        }
    };

    public T convertFromString(String s) {
        if (s == null) {
            return null;
        }

        return fromString(s);
    }

    public String convertToString(T value) {
        if (value == null) {
            return null;
        }

        return toString(value);
    }

    protected abstract T fromString(String s);

    protected abstract String toString(T value);
}
