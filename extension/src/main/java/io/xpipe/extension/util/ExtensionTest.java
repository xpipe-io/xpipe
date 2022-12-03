package io.xpipe.extension.util;

import io.xpipe.core.impl.FileStore;
import io.xpipe.core.store.DataStore;
import lombok.SneakyThrows;

import java.nio.file.Path;

public class ExtensionTest {

    @SneakyThrows
    public static Path getResourcePath(Class<?> c, String name) {
        var url = c.getResource(name);
        if (url == null) {
            throw new IllegalArgumentException(String.format("File %s does not exist", name));
        }
        var file = Path.of(url.toURI());
        return file;
    }

    @SneakyThrows
    public static DataStore getResourceStore(String name) {
        var url = ExtensionTest.class.getClassLoader().getResource(name);
        if (url == null) {
            throw new IllegalArgumentException(String.format("File %s does not exist", name));
        }
        var file = Path.of(url.toURI()).toString();
        return FileStore.local(Path.of(file));
    }
}
