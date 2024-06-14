package io.xpipe.ext.base.service;

import io.xpipe.app.util.LicenseConnectionLimit;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.LicensedFeature;
import io.xpipe.core.store.DataStore;

public class ServiceLicenseCheck {

    public static LicensedFeature getFeature() {
        return LicenseProvider.get().getFeature("services");
    }

    public static void check() {
        if (getFeature().isSupported()) {
            return;
        }

        var limit = getConnectionLimit();
        limit.checkLimit();
    }


    public static LicenseConnectionLimit getConnectionLimit() {
        // We check before starting a new service
        return new LicenseConnectionLimit(0, getFeature()) {

            @Override
            protected boolean matches(DataStore store) {
                return store instanceof AbstractServiceStore abstractServiceStore && abstractServiceStore.requiresTunnel() && abstractServiceStore.isSessionRunning();
            }
        };
    }
}
