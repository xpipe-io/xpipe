package io.xpipe.fxcomps.augment;

import io.xpipe.fxcomps.CompStructure;

public interface Augment<S extends CompStructure<?>> {

    void augment(S struc);
}
