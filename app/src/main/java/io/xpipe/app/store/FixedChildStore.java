package io.xpipe.app.store;

import java.util.OptionalInt;

public interface FixedChildStore extends DataStore {

    OptionalInt getFixedId();

    default FixedChildStore merge(FixedChildStore other) {
        return this;
    }
}
