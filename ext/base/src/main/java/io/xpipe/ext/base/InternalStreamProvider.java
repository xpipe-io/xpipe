package io.xpipe.ext.base;

import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.core.impl.InternalStreamStore;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;

import java.util.List;

public class InternalStreamProvider implements DataStoreProvider {

    @Override
    public boolean canManuallyCreate() {
        return false;
    }

    @Override
    public String queryInformationString(DataStore store, int length) {
        return getDisplayName();
    }

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
