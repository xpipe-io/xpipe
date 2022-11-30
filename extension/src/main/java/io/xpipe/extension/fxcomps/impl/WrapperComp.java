package io.xpipe.extension.fxcomps.impl;

import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;

import java.util.function.Supplier;

public class WrapperComp<S extends CompStructure<?>> extends Comp<S> {

    private final Supplier<S> structureSupplier;

    public WrapperComp(Supplier<S> structureSupplier) {
        this.structureSupplier = structureSupplier;
    }

    @Override
    public S createBase() {
        return structureSupplier.get();
    }
}
