package io.xpipe.core.store;

import java.util.Optional;

/**
 * Represents a store that has a filename.
 * Note that this does not only apply to file stores but any other store as well that has some kind of file name.
 */
public interface FilenameStore extends DataStore {

    @Override
    default Optional<String> determineDefaultName() {
        var n = getFileName();
        var i = n.lastIndexOf('.');
        return Optional.of(i != -1 ? n.substring(0, i) : n);
    }

    default String getFileExtension() {
        var split = getFileName().split("[\\\\.]");
        if (split.length == 0) {
            return "";
        }
        return split[split.length - 1];
    }

    String getFileName();
}
