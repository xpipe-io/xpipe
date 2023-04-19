package io.xpipe.core.dialog;

import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.util.SecretValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.Map;

public abstract class QueryConverter<T> {

    public static final QueryConverter<NewLine> NEW_LINE = new QueryConverter<NewLine>() {
        @Override
        protected NewLine fromString(String s) {
            return NewLine.byId(s);
        }

        @Override
        protected String toString(NewLine value) {
            return value.getId();
        }
    };

    public static final QueryConverter<StreamCharset> CHARSET = new QueryConverter<StreamCharset>() {
        @Override
        protected StreamCharset fromString(String s) {
            return StreamCharset.get(s);
        }

        @Override
        protected String toString(StreamCharset value) {
            return value.toString();
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

    public static final QueryConverter<SecretValue> SECRET = new QueryConverter<SecretValue>() {
        @Override
        protected SecretValue fromString(String s) {
            //TODO
            return null;
        }

        @Override
        protected String toString(SecretValue value) {
            return value.getSecretValue();
        }
    };

    public static final QueryConverter<Map.Entry<String, String>> HTTP_HEADER =
            new QueryConverter<Map.Entry<String, String>>() {
                @Override
                protected Map.Entry<String, String> fromString(String s) {
                    if (!s.contains(":")) {
                        throw new IllegalArgumentException("Missing colon");
                    }

                    var split = s.split(":");
                    if (split.length != 2) {
                        throw new IllegalArgumentException("Too many colons");
                    }

                    return new AbstractMap.SimpleEntry<>(split[0].trim(), split[1].trim());
                }

                @Override
                protected String toString(Map.Entry<String, String> value) {
                    return value.getKey() + ": " + value.getValue();
                }
            };

    public static final QueryConverter<URI> URI = new QueryConverter<URI>() {
        @Override
        protected URI fromString(String s) {
            try {
                return new URI(s);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        @Override
        protected String toString(URI value) {
            return value.toString();
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
