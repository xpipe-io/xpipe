package io.xpipe.app.comp.augment;

import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;

import javafx.scene.Node;
import javafx.scene.layout.Region;

public interface Augment<S extends CompStructure<?>> {

    @SuppressWarnings("unchecked")
    default void augment(Node r) {
        augment((S) new SimpleCompStructure<>((Region) r));
    }

    void augment(S struc);
}
