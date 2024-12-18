package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.util.JacksonMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    public static void clear(String key) {
        var path = getPath(key);
        if (Files.exists(path)) {
            FileUtils.deleteQuietly(path.toFile());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getNonNull(String key, Class<?> type, Supplier<T> notPresent) {
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
                if (r == null || !type.isAssignableFrom(r.getClass())) {
                    FileUtils.deleteQuietly(path.toFile());
                    return notPresent.get();
                } else {
                    return r;
                }
            } catch (Exception ex) {
                ErrorEvent.fromThrowable("Could not parse cached data for key " + key, ex)
                        .omit()
                        .handle();
                FileUtils.deleteQuietly(path.toFile());
            }
        }
        return notPresent != null ? notPresent.get() : null;
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
                ErrorEvent.fromThrowable("Could not parse cached data for key " + key, ex)
                        .omit()
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
            ErrorEvent.fromThrowable("Could not write cache data for key " + key, e)
                    .omitted(true)
                    .build()
                    .handle();
        }
    }
}
