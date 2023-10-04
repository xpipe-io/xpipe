package io.xpipe.app.test;

import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FileStore;
import lombok.SneakyThrows;

import java.nio.file.Path;

public class ExtensionTest {

    @SneakyThrows
    public static Path getResourcePath(Class<?> c, String name) {
        var url = c.getResource(name);
        if (url == null) {
            throw new IllegalArgumentException(String.format("File %s does not exist", name));
        }
        return Path.of(url.toURI());
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
