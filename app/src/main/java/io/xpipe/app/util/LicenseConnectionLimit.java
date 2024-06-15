package io.xpipe.app.util;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.store.DataStore;

public abstract class LicenseConnectionLimit {

    private final int limit;
    private final LicensedFeature feature;

    public LicenseConnectionLimit(int limit, LicensedFeature feature) {
        this.limit = limit;
        this.feature = feature;
    }

    protected abstract boolean matches(DataStore store);

    public void checkLimit() {
        if (feature.isSupported()) {
            return;
        }

        var found = DataStorage.get().getStoreEntries().stream()
                .filter(entry -> entry.getValidity().isUsable() && matches(entry.getStore()))
                .toList();
        if (found.size() > limit) {
            throw new LicenseRequiredException(feature, limit);
        }
    }
}
