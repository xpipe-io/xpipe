package io.xpipe.extension.fxcomps.augment;

import io.xpipe.extension.fxcomps.CompStructure;

public interface Augment<S extends CompStructure<?>> {

    void augment(S struc);
}
