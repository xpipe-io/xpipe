package io.xpipe.core.store;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@EqualsAndHashCode
public final class FilePath {

    @NonNull
    private final String value;

    public FilePath(@NonNull String value) {
        this.value = value;
        if (value.isBlank()) {
            throw new IllegalArgumentException();
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException();
        }
    }

    public Path toLocalPath() {
        return Path.of(value);
    }

    public String toString() {
        return value;
    }

    public String quoteIfNecessary() {
        return value.contains(" ") ? "\"" + value + "\"" : value;
    }

    public FilePath toDirectory() {
        if (value.endsWith("/") || value.endsWith("\\")) {
            return new FilePath(value);
        }

        if (value.contains("\\")) {
            return new FilePath(value + "\\");
        }

        return new FilePath(value + "/");
    }

    public FilePath removeTrailingSlash() {
        if (value.equals("/")) {
            return new FilePath(value);
        }

        if (value.endsWith("/") || value.endsWith("\\")) {
            return new FilePath(value.substring(0, value.length() - 1));
        }
        return new FilePath(value);
    }

    public String getFileName() {
        var split = value.split("[\\\\/]");
        if (split.length == 0) {
            return "";
        }
        var components = Arrays.stream(split).filter(s -> !s.isEmpty()).toList();
        if (components.size() == 0) {
            return "";
        }

        return components.getLast();
    }

    public List<String> splitHierarchy() {
        var f = value + "/";
        var list = new ArrayList<String>();
        int lastElementStart = 0;
        for (int i = 0; i < f.length(); i++) {
            if (f.charAt(i) == '\\' || f.charAt(i) == '/') {
                if (i - lastElementStart > 0) {
                    list.add(f.substring(0, i));
                }

                lastElementStart = i + 1;
            }
        }
        return list;
    }

    public String getBaseName() {
        var split = value.lastIndexOf(".");
        if (split == -1) {
            return value;
        }
        return value.substring(0, split);
    }

    public String getExtension() {
        var name = FileNames.getFileName(value);
        var split = name.split("\\.");
        if (split.length == 0) {
            return null;
        }
        return split[split.length - 1];
    }

    public FilePath join(String... parts) {
        var joined = String.join("/", parts);
        return new FilePath(value + "/" + joined).normalize();
    }

    public boolean isAbsolute() {
        if (!value.contains("/") && !value.contains("\\")) {
            return false;
        }

        if (!value.startsWith("\\") && !value.startsWith("/") && !value.startsWith("~") && !value.matches("^\\w:.*")) {
            return false;
        }

        return true;
    }

    public FilePath getParent() {
        if (split().size() == 0) {
            return null;
        }

        if (split().size() == 1) {
            return value.startsWith("/") && !value.equals("/") ? new FilePath("/") : null;
        }

        return new FilePath(value.substring(0, value.length() - getFileName().length() - 1));
    }

    public boolean startsWith(FilePath start) {
        return normalize().startsWith(start.normalize());
    }

    public FilePath relativize(FilePath base) {
        return new FilePath(normalize()
                .toString()
                .substring(base.normalize().toDirectory().toString().length()));
    }

    public FilePath normalize() {
        var backslash = value.contains("\\");
        return new FilePath(backslash ? toWindows() : toUnix());
    }

    private List<String> split() {
        var split = value.split("[\\\\/]");
        return Arrays.stream(split).filter(s -> !s.isEmpty()).toList();
    }

    public String toUnix() {
        var joined = String.join("/", split());
        var prefix = value.startsWith("/") ? "/" : "";
        var suffix = value.endsWith("/") || value.endsWith("\\") ? "/" : "";
        return prefix + joined + suffix;
    }

    public String toWindows() {
        var suffix = value.endsWith("/") || value.endsWith("\\") ? "\\" : "";
        return String.join("\\", split()) + suffix;
    }
}
