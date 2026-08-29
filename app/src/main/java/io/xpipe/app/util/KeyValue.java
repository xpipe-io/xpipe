package io.xpipe.app.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KeyValue {

    public static KeyValue raw(String key, String value) {
        return new KeyValue(key, value);
    }

    public static KeyValue escape(String key, Object value) {
        var string = value.toString();

        string = string.replaceAll("\\\\", "\\\\\\\\");

        var isQuoted = string.startsWith("\"") && string.endsWith("\"");
        if (!isQuoted && string.contains(" ")) {
            string = "\"" + string + "\"";
        }

        return new KeyValue(key, string);
    }

    String key;
    String value;
}
