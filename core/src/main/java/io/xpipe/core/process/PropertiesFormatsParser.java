package io.xpipe.core.process;

import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

public class PropertiesFormatsParser {

    @SneakyThrows
    public static Map<String, String> parse(String text, String split) {
        var map = new LinkedHashMap<String, String>();

        var reader = new BufferedReader(new StringReader(text));
        String line;

        String currentKey = null;
        StringBuilder currentValue = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                currentValue.append(line);
                continue;
            }

            if (!line.contains(split)) {
                continue;
            }

            var keyName = line.substring(0, line.indexOf(split)).strip();
            var value = line.substring(line.indexOf(split) + 1).strip();
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }

            if (currentKey != null) {
                map.put(currentKey, currentValue.toString());
            }

            currentKey = keyName;
            currentValue = new StringBuilder(value);
        }

        if (currentKey != null) {
            map.put(currentKey, currentValue.toString());
        }

        return map;
    }
}
