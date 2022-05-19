package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

@JsonTypeName("localDir")
@EqualsAndHashCode
public class LocalDirectoryDataStore implements DataStore {

    private final Path file;

    @JsonCreator
    public LocalDirectoryDataStore(Path file) {
        this.file = file;
    }

    @Override
    public Optional<String> determineDefaultName() {
        return Optional.of(file.getFileName().toString());
    }

    @Override
    public Optional<Instant> determineLastModified() {
        try {
            var l = Files.getLastModifiedTime(file);
            return Optional.of(l.toInstant());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public Path getPath() {
        return file;
    }

}
