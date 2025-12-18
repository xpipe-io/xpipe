package io.xpipe.app.util;

import lombok.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
public class IniFile {

    private static final Pattern SECTION_FORMAT = Pattern.compile("\\s*\\[([^]]*)\\]\\s*");
    private static final Pattern VALUE_FORMAT = Pattern.compile("\\s*([^=]*)=(.*)");

     Map<String, Map<String, String>> entries;

    public static IniFile load(Path path) throws IOException {
        Map<String, Map<String, String>> entries = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            String section = null;
            while ((line = br.readLine()) != null) {
                Matcher m = SECTION_FORMAT.matcher(line);
                if (m.matches()) {
                    section = m.group(1).strip();
                } else if (section != null) {
                    m = VALUE_FORMAT.matcher(line);
                    if (m.matches()) {
                        String key = m.group(1).strip();
                        String value = m.group(2).strip();
                        Map<String, String> kv = entries.computeIfAbsent(section, k -> new HashMap<>());
                        kv.put(key, value);
                    }
                }
            }
        }
        return new IniFile(entries);
    }

    public String get(String section, String key) {
        Map<String, String> kv = entries.get(section);
        if (kv == null) {
            return null;
        }
        return kv.get(key);
    }

    public String getOrDefault(String section, String key, String defaultvalue) {
        Map<String, String> kv = entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        return kv.get(key);
    }
}
