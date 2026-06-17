package io.xpipe.ext.base.service;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;

import java.util.Optional;

public class ServiceHelper {

    public static Optional<DataStoreEntryRef<MappedServiceStore>> findMappedServiceStore(DataStoreEntryRef<?> ref, int containerPort) {
        var children = DataStorage.get().getStoreChildren(ref.get());
        var foundServiceGroup = children.stream().filter(entry -> entry.getStore() instanceof AbstractServiceGroupStore<?>).findFirst();
        if (foundServiceGroup.isEmpty()) {
            return Optional.empty();
        }

        var foundService = DataStorage.get().getStoreChildren(foundServiceGroup.get()).stream()
                .filter(entry -> entry.getStore() instanceof MappedServiceStore m && m.getContainerPort() == containerPort)
                .filter(entry -> entry.getValidity().isUsable())
                .map(entry -> entry.<MappedServiceStore>ref())
                .findFirst();
        return foundService;
    }
}
