package io.xpipe.core.store;

import java.util.Arrays;
import java.util.List;

public class FileNames {

    public static String join(String... parts) {
        var joined = String.join("/", parts);
        return normalize(joined);
    }

    public static boolean isAbsolute(String file) {
        if (!file.contains("/") && !file.contains("\\")) {
            return false;
        }

        if (!file.startsWith("\\") && !file.startsWith("/") && !file.startsWith("~") && !file.matches("^\\w:.*")) {
            return false;
        }

        return true;
    }

    public static String normalize(String file) {
        var backslash = file.contains("\\");
        return backslash ? toWindows(file) : toUnix(file);
    }

    private static List<String> split(String file) {
        var split = file.split("[\\\\/]");
        return Arrays.stream(split).filter(s -> !s.isEmpty()).toList();
    }

    public static String toUnix(String file) {
        var joined = String.join("/", split(file));
        var prefix = file.startsWith("/") ? "/" : "";
        var suffix = file.endsWith("/") || file.endsWith("\\") ? "/" : "";
        return prefix + joined + suffix;
    }

    public static String toWindows(String file) {
        var suffix = file.endsWith("/") || file.endsWith("\\") ? "\\" : "";
        return String.join("\\", split(file)) + suffix;
    }
}
