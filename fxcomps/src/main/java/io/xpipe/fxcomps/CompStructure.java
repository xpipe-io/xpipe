package io.xpipe.fxcomps;

import javafx.scene.layout.Region;

public interface CompStructure<R extends Region> {
    R get();
}
