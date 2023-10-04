package io.xpipe.ext.base;

import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.core.store.InternalStreamStore;
import io.xpipe.core.store.DataStore;

import java.util.List;

public class InternalStreamProvider implements DataStoreProvider {

    @Override
    public DataStore defaultStore() {
        return new InternalStreamStore();
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("internalStream");
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(InternalStreamStore.class);
    }
}
