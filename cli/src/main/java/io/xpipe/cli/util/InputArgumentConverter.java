package io.xpipe.cli.util;

import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

public class InputArgumentConverter implements CommandLine.ITypeConverter<String> {

    public static Optional<Path> fileIfPresent(String s) {
        try {
            var file = file(s);
            if (Files.exists(file)) {
                return Optional.of(file);
            }
        } catch (InvalidPathException ex) {
        }
        return Optional.empty();
    }

    public static Path file(String s) {
        var file = Path.of(s);
        if (file.isAbsolute()) {
            return file;
        } else {
            var abs = Path.of(System.getProperty("user.dir")).resolve(file);
            return abs;
        }
    }

    @Override
    public String convert(String value) throws Exception {
        var file = fileIfPresent(value);
        if (file.isPresent()) {
            return file.get().toString();
        }

        return value;
    }
}
