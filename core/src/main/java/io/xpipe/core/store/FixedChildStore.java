package io.xpipe.core.store;

import java.util.OptionalInt;

public interface FixedChildStore extends DataStore {

    OptionalInt getFixedId();
}
