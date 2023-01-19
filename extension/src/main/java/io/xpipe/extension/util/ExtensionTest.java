package io.xpipe.extension.util;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.store.DataStore;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;

import java.nio.file.Path;

public class ExtensionTest {

    public static void assertNodeEquals(DataStructureNode expected, DataStructureNode actual) {
        if (expected.isValue() || actual.isValue()) {
            Assertions.assertEquals(expected, actual);
        } else {
            for (int i = 0; i < Math.min(expected.size(), actual.size()); i++) {
                Assertions.assertEquals(expected.getNodes().get(i), actual.getNodes().get(i));
            }
            Assertions.assertEquals(expected, actual);
        }
    }

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
