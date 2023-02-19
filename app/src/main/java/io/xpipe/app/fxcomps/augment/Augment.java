package io.xpipe.app.fxcomps.augment;

import io.xpipe.app.fxcomps.CompStructure;

public interface Augment<S extends CompStructure<?>> {

    void augment(S struc);
}
