package io.xpipe.app.hub.comp;

import lombok.Value;

import java.util.Arrays;
import java.util.List;

@Value
public class StoreFilter {

    public static StoreFilter of(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }

        var split = s.split(",");
        var l = Arrays.stream(split)
                .map(sub -> sub.strip())
                .filter(sub -> !sub.isEmpty())
                .toList();
        return new StoreFilter(l);
    }

    List<String> parts;

    public boolean matches(List<String> input) {
        if (input == null) {
            return false;
        }

        for (String part : parts) {
            var partl = part.toLowerCase();
            var found = false;
            for (String s : input) {
                if (s == null || s.isEmpty()) {
                    continue;
                }

                var sl = s.toLowerCase();
                if (sl.contains(partl)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }
}
