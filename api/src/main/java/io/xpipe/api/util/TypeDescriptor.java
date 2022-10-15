package io.xpipe.api.util;

import java.util.List;
import java.util.stream.Collectors;

public class TypeDescriptor {

    public static String create(List<String> names) {
        return "[" + names.stream().map(n -> n != null ? "\"" + n + "\"" : null).collect(Collectors.joining(","))
                + "]\n";
    }
}
