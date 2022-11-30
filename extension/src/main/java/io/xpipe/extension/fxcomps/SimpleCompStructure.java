package io.xpipe.extension.fxcomps;

import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class SimpleCompStructure<R extends Region> implements CompStructure<R> {

    R value;

    @Override
    public R get() {
        return value;
    }
}
