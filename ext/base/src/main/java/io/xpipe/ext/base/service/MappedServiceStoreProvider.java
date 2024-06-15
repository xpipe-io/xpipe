package io.xpipe.ext.base.service;

import io.xpipe.app.comp.store.StoreEntryWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class MappedServiceStoreProvider extends FixedServiceStoreProvider {

    @Override
    public List<String> getPossibleNames() {
        return List.of("mappedService");
    }

    @Override
    public ObservableValue<String> informationString(StoreEntryWrapper wrapper) {
        MappedServiceStore s = wrapper.getEntry().getStore().asNeeded();
        return new SimpleStringProperty("Port " + s.getContainerPort() + " -> " + s.getRemotePort());
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(MappedServiceStore.class);
    }
}
