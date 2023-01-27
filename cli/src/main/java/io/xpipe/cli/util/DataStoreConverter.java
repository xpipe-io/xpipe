package io.xpipe.cli.util;

import io.xpipe.core.impl.FileStore;
import io.xpipe.core.impl.NamedStore;
import io.xpipe.core.store.DataStore;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

public class DataStoreConverter implements CommandLine.ITypeConverter<DataStore> {

    private static final Pattern NAME_PATTERN = Pattern.compile("\\w+");

    public static Optional<FileStore> fileIfPresent(String s) {
        try {
            var file = file(s);
            if (Files.exists(Path.of(file.getFile()))) {
                return Optional.of(file);
            }
        } catch (InvalidPathException ex) {
        }
        return Optional.empty();
    }

    public static FileStore file(String s) {
        var file = Path.of(s);
        if (file.isAbsolute()) {
            return FileStore.local(file);
        } else {
            var abs = Path.of(System.getProperty("user.dir")).resolve(file);
            return FileStore.local(abs);
        }
    }

    @Override
    public DataStore convert(String value) throws Exception {
        var file = fileIfPresent(value);
        if (file.isPresent()) {
            return file.get();
        }

        if (NAME_PATTERN.matcher(value).matches()) {
            return NamedStore.builder().name(value).build();
        }

        return file(value);
    }
}
