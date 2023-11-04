package io.xpipe.core.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Identifiers {

    @SafeVarargs
    public static List<String> join(List<String>... s) {
        return Arrays.stream(s).flatMap(Collection::stream).toList();
    }

    public static List<String> get(String... s) {
        return nameAlternatives(Arrays.asList(s));
    }

    private static List<String> nameAlternatives(List<String> split) {
        return List.of(String.join("", split), String.join(" ", split), String.join("_", split), String.join("-", split));
    }
}
