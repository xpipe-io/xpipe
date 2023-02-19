package io.xpipe.ext.base;

import io.xpipe.core.source.DataSource;
import io.xpipe.core.store.MachineStore;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
public class MachineRootContainer extends SimpleCollectionSource {

    MachineStore store;

    @Override
    protected List<DataSource<?>> get() throws Exception {
        return List.of();
    }
}
