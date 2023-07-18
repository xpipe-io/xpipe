package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.store.DataStore;
import lombok.EqualsAndHashCode;

import java.nio.file.Path;

@JsonTypeName("localDir")
@EqualsAndHashCode
public class LocalDirectoryDataStore implements DataStore {

    private final Path file;

    @JsonCreator
    public LocalDirectoryDataStore(Path file) {
        this.file = file;
    }

    public Path getPath() {
        return file;
    }
}
