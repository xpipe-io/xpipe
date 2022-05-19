package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

@JsonTypeName("local")
@EqualsAndHashCode
public class LocalFileDataStore implements FileDataStore {

    private final Path file;

    @JsonCreator
    public LocalFileDataStore(Path file) {
        this.file = file;
    }

    public String toString() {
        return getFileName();
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

    public Path getFile() {
        return file;
    }

    @Override
    public InputStream openInput() throws Exception {
        return new BufferedInputStream(Files.newInputStream(file));
    }

    @Override
    public OutputStream openOutput() throws Exception {
        return Files.newOutputStream(file);
    }

    @Override
    public boolean exists() {
        return Files.exists(file);
    }

    @Override
    public String getFileName() {
        return file.getFileName().toString();
    }
}
