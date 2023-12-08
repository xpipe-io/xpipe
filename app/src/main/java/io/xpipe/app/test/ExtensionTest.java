package io.xpipe.app.test;

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
}
