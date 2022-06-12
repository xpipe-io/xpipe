package io.xpipe.core.config;

import java.nio.charset.Charset;

public abstract class QueryConverter<T> {

    public static final QueryConverter<Charset> CHARSET = new QueryConverter<Charset>() {
        @Override
        protected Charset fromString(String s) {
            return Charset.forName(s);
        }

        @Override
        protected String toString(Charset value) {
            return value.name();
        }
    };

    public static final QueryConverter<String> STRING = new QueryConverter<String>() {
        @Override
        protected String fromString(String s) {
            return s;
        }

        @Override
        protected String toString(String value) {
            return value;
        }
    };

    public static final QueryConverter<Integer> INTEGER = new QueryConverter<Integer>() {
        @Override
        protected Integer fromString(String s) {
            return Integer.parseInt(s);
        }

        @Override
        protected String toString(Integer value) {
            return value.toString();
        }
    };

    public static final QueryConverter<Character> CHARACTER = new QueryConverter<Character>() {
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

    public static final QueryConverter<Boolean> BOOLEAN = new QueryConverter<Boolean>() {
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
