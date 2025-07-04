package io.xpipe.app.ext;

import java.util.OptionalInt;

public interface FixedChildStore extends DataStore {

    OptionalInt getFixedId();

    default FixedChildStore merge(FixedChildStore other) {
        return this;
    }
}
