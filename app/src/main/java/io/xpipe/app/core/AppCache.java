package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.core.JacksonMapper;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

public class AppCache {

    @Getter
    @Setter
    private static Path basePath;

    private static Path getPath(String key) {
        var name = key + ".cache";
        return getBasePath().resolve(name);
    }

    public static void clear() {
        if (!Files.exists(getBasePath())) {
            return;
        }

        try {
            FileUtils.cleanDirectory(getBasePath().toFile());
        } catch (IOException e) {
            ErrorEventFactory.fromThrowable(e).handle();
        }
    }

    public static void clear(String key) {
        var path = getPath(key);
        if (Files.exists(path)) {
            FileUtils.deleteQuietly(path.toFile());
        }
    }

    public static <T> T getNonNull(String key, Class<?> type, Supplier<T> notPresent) {
        return getNonNull(key, TypeFactory.defaultInstance().constructType(type), notPresent);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getNonNull(String key, JavaType type, Supplier<T> notPresent) {
        var path = getPath(key);
        if (Files.exists(path)) {
            try {
                ObjectMapper o = JacksonMapper.getDefault();
                var tree = o.readTree(path.toFile());
                if (tree == null || tree.isMissingNode() || tree.isNull()) {
                    FileUtils.deleteQuietly(path.toFile());
                    return notPresent.get();
                }

                var r = (T) JacksonMapper.getDefault().treeToValue(tree, type);
                if (r == null) {
                    FileUtils.deleteQuietly(path.toFile());
                    return notPresent.get();
                } else {
                    return r;
                }
            } catch (Exception ex) {
                ErrorEventFactory.fromThrowable("Could not parse cached data for key " + key, ex)
                        .omit()
                        .expected()
                        .handle();
                FileUtils.deleteQuietly(path.toFile());
            }
        }
        return notPresent.get();
    }

    public static boolean getBoolean(String key, boolean notPresent) {
        var path = getPath(key);
        if (Files.exists(path)) {
            try {
                ObjectMapper o = JacksonMapper.getDefault();
                var tree = o.readTree(path.toFile());
                if (tree == null || !tree.isBoolean()) {
                    FileUtils.deleteQuietly(path.toFile());
                    return notPresent;
                }

                return tree.asBoolean();
            } catch (Exception ex) {
                ErrorEventFactory.fromThrowable("Could not parse cached data for key " + key, ex)
                        .omit()
                        .expected()
                        .handle();
                FileUtils.deleteQuietly(path.toFile());
            }
        }
        return notPresent;
    }

    public static <T> void update(String key, T val) {
        var path = getPath(key);

        try {
            FileUtils.forceMkdirParent(path.toFile());
            JacksonMapper.getDefault().writeValue(path.toFile(), val);
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable("Could not write cache data for key " + key, e)
                    .omitted(true)
                    .expected()
                    .build()
                    .handle();
        }
    }

    public static Optional<Instant> getModifiedTime(String key) {
        var path = getPath(key);
        if (Files.exists(path)) {
            try {
                var t = Files.getLastModifiedTime(path);
                return Optional.of(t.toInstant());
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable("Could not get modified date for " + key, e)
                        .omitted(true)
                        .expected()
                        .build()
                        .handle();
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }
}
