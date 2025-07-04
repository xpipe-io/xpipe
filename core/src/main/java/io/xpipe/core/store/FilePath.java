package io.xpipe.core.store;

import io.xpipe.core.process.OsType;

import lombok.NonNull;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public final class FilePath {

    public static boolean isProbableFilePath(OsType osType, String s) {
        if (osType.equals(OsType.WINDOWS) && s.length() >= 2 && s.charAt(1) == ':') {
            return true;
        }

        return s.startsWith("/");
    }

    public static FilePath of(String path) {
        return path != null ? new FilePath(path) : null;
    }

    public static FilePath of(String... path) {
        if (path == null || path.length == 0) {
            return null;
        }

        var cp = Arrays.stream(path).skip(1).toArray(String[]::new);
        return path.length > 1 ? new FilePath(path[0]).join(cp) : new FilePath(path[0]);
    }

    public static FilePath of(Path path) {
        return path != null ? new FilePath(path.toString()) : null;
    }

    @NonNull
    private final String value;

    private FilePath normalized;
    private List<String> split;

    private FilePath(@NonNull String value) {
        this.value = value;
        if (value.isBlank()) {
            throw new IllegalArgumentException();
        }
        if (!value.equals(value.strip())) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FilePath filePath = (FilePath) o;
        return Objects.equals(
                normalize().removeTrailingSlash().value, filePath.normalize().removeTrailingSlash().value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    public FilePath fileSystemCompatible(OsType osType) {
        var split = split();
        var needsReplacement = split.stream().anyMatch(s -> !s.equals(osType.makeFileSystemCompatible(s)));
        if (!needsReplacement) {
            return this;
        }

        var p = Pattern.compile("[^/\\\\]+");
        var m = p.matcher(value);
        var replaced = m.replaceAll(matchResult -> osType.makeFileSystemCompatible(matchResult.group()));
        return FilePath.of(replaced);
    }

    public FilePath getRoot() {
        if (value.startsWith("/")) {
            return FilePath.of("/");
        } else if (value.length() >= 2 && value.charAt(1) == ':') {
            // Without the trailing slash, many programs struggle with this
            return FilePath.of(value.substring(0, 2) + "\\");
        } else if (value.startsWith("\\\\")) {
            var split = split();
            if (split.size() > 0) {
                return FilePath.of("\\\\" + split.getFirst());
            }
        }

        throw new IllegalArgumentException("Unable to determine root of " + value);
    }

    public Path toLocalPath() {
        return Path.of(value);
    }

    public String toString() {
        return value;
    }

    public FilePath toDirectory() {
        if (value.endsWith("/") || value.endsWith("\\")) {
            return FilePath.of(value);
        }

        if (value.contains("\\")) {
            return FilePath.of(value + "\\");
        }

        return FilePath.of(value + "/");
    }

    public FilePath removeTrailingSlash() {
        if (value.equals("/")) {
            return FilePath.of(value);
        }

        if (value.endsWith("/") || value.endsWith("\\")) {
            return FilePath.of(value.substring(0, value.length() - 1));
        }
        return FilePath.of(value);
    }

    public String getFileName() {
        var split = split();
        if (split.size() == 0) {
            return "";
        }
        var components = split.stream().filter(s -> !s.isEmpty()).toList();
        if (components.size() == 0) {
            return "";
        }

        return components.getLast();
    }

    public FilePath getBaseName() {
        var split = value.lastIndexOf(".");
        if (split == -1) {
            return this;
        }
        return FilePath.of(value.substring(0, split));
    }

    public Optional<String> getExtension() {
        var name = getFileName();
        var split = name.split("\\.");
        if (split.length < 2) {
            return Optional.empty();
        }
        return Optional.of(split[split.length - 1]);
    }

    public FilePath join(String... parts) {
        var joined = String.join("/", parts);
        return FilePath.of(value + "/" + joined).normalize();
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
        var split = split();
        if (split.size() == 0) {
            return this;
        }

        if (split.size() == 1) {
            return value.startsWith("/") && !value.equals("/") ? FilePath.of("/") : this;
        }

        return FilePath.of(value.substring(0, value.length() - getFileName().length() - 1));
    }

    public boolean startsWith(String start) {
        return startsWith(FilePath.of(start));
    }

    public boolean startsWith(FilePath start) {
        return normalize().toString().startsWith(start.normalize().toString());
    }

    public FilePath relativize(FilePath base) {
        return FilePath.of(normalize()
                .toString()
                .substring(base.normalize().toDirectory().toString().length()));
    }

    public FilePath normalize() {
        if (normalized != null) {
            return normalized;
        }

        var backslash = value.contains("\\");
        var r = backslash ? toWindows() : toUnix();
        normalized = r;
        return r;
    }

    public FilePath resolveTildeHome(String dir) {
        return value.startsWith("~") ? FilePath.of(value.replace("~", dir)) : this;
    }

    public List<String> split() {
        if (split != null) {
            return split;
        }

        var ar = value.split("[\\\\/]");
        var l = Arrays.stream(ar).filter(s -> !s.isEmpty()).toList();
        split = l;
        return l;
    }

    public FilePath toUnix() {
        if (value.equals("/")) {
            return this;
        }

        var joined = String.join("/", split());
        var prefix = value.startsWith("/") ? "/" : "";
        var suffix = value.endsWith("/") || value.endsWith("\\") ? "/" : "";
        return FilePath.of(prefix + joined + suffix);
    }

    public FilePath toWindows() {
        var suffix = value.endsWith("/") || value.endsWith("\\") ? "\\" : "";
        return FilePath.of(String.join("\\", split()) + suffix);
    }

    public Path asLocalPath() {
        return Path.of(value);
    }
}
