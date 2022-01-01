package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

@JsonTypeName("local")
public class LocalFileDataStore extends FileDataStore {

    private final Path file;

    @JsonCreator
    public LocalFileDataStore(Path file) {
        this.file = file;
    }

    @Override
    public Optional<String> determineDefaultName() {
        return Optional.of(file.getFileName().toString());
    }

    @Override
    public Optional<Instant> getLastModified() {
        try {
            var l = Files.getLastModifiedTime(file);
            return Optional.of(l.toInstant());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public String getName() {
        return file.getFileName().toString();
    }

    @Override
    @JsonIgnore
    public boolean isLocal() {
        return true;
    }

    @Override
    public LocalFileDataStore getLocal() {
        return this;
    }

    @Override
    public RemoteFileDataStore getRemote() {
        throw new UnsupportedOperationException();
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
}
