package io.xpipe.core.impl;

import java.util.Arrays;
import java.util.List;

public class FileNames {

    public static String toDirectory(String path) {
        if (path.endsWith("/") || path.endsWith("\\")) {
            return path;
        }

        if (path.contains("\\")) {
            return path + "\\";
        }

        return path + "/";
    }

    public static String removeTrailingSlash(String path) {
        if (path.equals("/")) {
            return path;
        }

        if (path.endsWith("/") || path.endsWith("\\")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    public static String getFileName(String file) {
        if (file.isEmpty()) {
            return "";
        }

        var split = file.split("[\\\\/]");
        if (split.length == 0) {
            return "";
        }
        var components =  Arrays.stream(split).filter(s -> !s.isEmpty()).toList();
        if (components.size() == 0) {
            return "";
        }

        return components.get(components.size() - 1);
    }

    public static String getExtension(String file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        var name = FileNames.getFileName(file);
        var split = file.split("\\.");
        if (split.length == 0) {
            return null;
        }
        return split[split.length - 1];
    }

    public static String join(String... parts) {
        var joined = String.join("/", parts);
        return normalize(joined);
    }

    public static boolean isAbsolute(String file) {
        if (!file.contains("/") && !file.contains("\\")) {
            return false;
        }

        if (!file.startsWith("/") && !file.startsWith("~") && !file.matches("^\\w:.*")) {
            return false;
        }

        return true;
    }

    public static String getParent(String file) {
        if (split(file).size() == 0) {
            return null;
        }

        if (split(file).size() == 1) {
            return file.startsWith("/") && !file.equals("/") ? "/" : null;
        }

        return file.substring(0, file.length() - getFileName(file).length() - 1);
    }

    public static boolean startsWith(String file, String start) {
        return normalize(file).startsWith(normalize(start));
    }

    public static String relativize(String from, String to) {
       return normalize(to).substring(normalize(from).length());
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
        var prefix =  file.startsWith("/") ? "/" : "";
        var suffix = file.endsWith("/") || file.endsWith("\\") ? "/" : "";
        return prefix + joined + suffix;
    }

    public static String toWindows(String file) {
        var suffix = file.endsWith("/") || file.endsWith("\\") ? "\\" : "";
        return String.join("\\", split(file)) + suffix;
    }
}
