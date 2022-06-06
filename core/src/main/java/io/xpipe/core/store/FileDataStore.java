package io.xpipe.core.store;

import java.util.Optional;

public interface FileDataStore extends StreamDataStore {

    @Override
    default Optional<String> determineDefaultName() {
        var n = getFileName();
        var i = n.lastIndexOf('.');
        return Optional.of(i != -1 ? n.substring(0, i) : n);
    }

    @Override
    default boolean persistent() {
        return true;
    }

    String getFileName();
}
