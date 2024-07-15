package io.xpipe.ext.base.service;

import io.xpipe.app.comp.store.StoreSection;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class MappedServiceStoreProvider extends FixedServiceStoreProvider {

    @Override
    public List<String> getPossibleNames() {
        return List.of("mappedService");
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        MappedServiceStore s = section.getWrapper().getEntry().getStore().asNeeded();
        return new SimpleStringProperty("Port " + s.getRemotePort() + " -> " + s.getContainerPort());
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(MappedServiceStore.class);
    }
}
