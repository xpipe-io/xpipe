package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.JsonConfigHelper;
import io.xpipe.core.util.JacksonMapper;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

public class AppCache {

    public static <T> Optional<T> getIfPresent(String key, Class<T> type) {
        return Optional.ofNullable(get(key, type, () -> null));
    }

    private static Path getBasePath() {
        return AppProperties.get().getDataDir().resolve("cache");
    }

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
    public static <T> T get(String key, Class<?> type, Supplier<T> notPresent) {
        var path = getPath(key);
        if (Files.exists(path)) {
            try {
                var tree = JsonConfigHelper.readRaw(path);
                if (tree.isMissingNode()) {
                    return notPresent.get();
                }

                return (T) JacksonMapper.getDefault().treeToValue(tree, type);
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).omit().handle();
                FileUtils.deleteQuietly(path.toFile());
            }
        }
        return notPresent != null ? notPresent.get() : null;
    }

    public static <T> void update(String key, T val) {
        var path = getPath(key);

        try {
            FileUtils.forceMkdirParent(path.toFile());
            var tree = JacksonMapper.getDefault().valueToTree(val);
            JsonConfigHelper.writeConfig(path, tree);
        } catch (Exception e) {
            ErrorEvent.fromThrowable("Could not parse cached data for key " + key, e)
                    .omitted(true)
                    .build()
                    .handle();
        }
    }

    public <T> T getValue(String key, Class<?> type, Supplier<T> notPresent) {
        return get(key, type, notPresent);
    }

    public <T> void updateValue(String key, T val) {
        update(key, val);
    }
}
