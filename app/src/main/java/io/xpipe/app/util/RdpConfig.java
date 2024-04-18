package io.xpipe.app.util;

import io.xpipe.core.util.StreamCharset;
import lombok.Value;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
public class RdpConfig {

    Map<String, TypedValue> content;

    public static RdpConfig parseFile(String file) throws Exception {
        try (var in = new BufferedReader(StreamCharset.detectedReader(new BufferedInputStream(Files.newInputStream(Path.of(file)))))) {
            var content = in.lines().collect(Collectors.joining("\n"));
            return parseContent(content);
        }
    }

    public static RdpConfig parseContent(String content) {
        var map = new LinkedHashMap<String, TypedValue>();
        content.lines().forEach(s -> {
            var split = s.split(":");
            if (split.length < 2) {
                return;
            }

            if (split.length == 2) {
                map.put(split[0].trim(), new RdpConfig.TypedValue("s", split[1].trim()));
            }

            if (split.length == 3) {
                map.put(split[0].trim(), new RdpConfig.TypedValue(split[1].trim(), split[2].trim()));
            }
        });
        return new RdpConfig(map);
    }

    public RdpConfig overlay(Map<String, TypedValue> override) {
        var newMap = new LinkedHashMap<>(content);
        newMap.putAll(override);
        return new RdpConfig(newMap);
    }

    public String toString() {
        return content.entrySet().stream()
                .map(e -> {
                    return e.getKey() + ":" + e.getValue().getType() + ":"
                            + e.getValue().getValue();
                })
                .collect(Collectors.joining("\n"));
    }

    public Optional<TypedValue> get(String key) {
        return Optional.ofNullable(content.get(key));
    }

    @Value
    public static class TypedValue {
        String type;
        String value;

        public static TypedValue string(String value) {
            return new TypedValue("s", value);
        }
    }
}
