package io.xpipe.core.impl;

import java.util.Arrays;
import java.util.List;

public class FileNames {

    public static String getFileName(String file) {
        var split = file.split("[\\\\/]");
        if (split.length == 0) {
            return "";
        }
        return split[split.length - 1];
    }

    public static String join(String... parts) {
        var joined = String.join("/", parts);
        return normalize(joined);
    }

    public static String normalize(String file) {
        var backslash = file.contains("\\");
        return backslash ? toWindows(file) : toUnix(file);
    }

    private  static List<String> split(String file) {
        var split = file.split("[\\\\/]");
        return Arrays.stream(split).filter(s -> !s.isEmpty()).toList();
    }

    public static String toUnix(String file) {
        var joined = String.join("/", split(file));
        return file.startsWith("/") ? "/" + joined : joined;
    }

    public static String toWindows(String file) {
        return String.join("\\", split(file));
    }
}
