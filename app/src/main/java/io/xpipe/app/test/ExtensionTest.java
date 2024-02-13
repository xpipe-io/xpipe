package io.xpipe.app.test;

import io.xpipe.core.store.FileNames;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;

public class ExtensionTest {

    @SneakyThrows
    public static Path getResourcePath(Class<?> c, String name) {
        var loc = Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI());
        var testName = FileNames.getBaseName(loc.getFileName().toString()).split("-")[1];
        var f = loc.getParent().getParent().resolve("resources").resolve(testName).resolve(name);
        if (!Files.exists(f)) {
            throw new IllegalArgumentException(String.format("File %s does not exist", name));
        }
        return f;
    }
}
