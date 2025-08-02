package io.xpipe.app.util;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.ext.ExtensionException;
import io.xpipe.core.ModuleLayerLoader;

import javafx.beans.value.ObservableValue;

import java.util.ServiceLoader;

public abstract class LicenseProvider {

    private static LicenseProvider INSTANCE = null;

    public static LicenseProvider get() {
        return INSTANCE;
    }

    public abstract void updateDate(String date);

    public abstract String formatExceptionMessage(String name, boolean plural, LicensedFeature licensedFeature);

    public abstract boolean hasLicense();

    public abstract String getLicenseId();

    public abstract ObservableValue<String> licenseTitle();

    public abstract LicensedFeature getFeature(String id);

    public abstract LicensedFeature checkOsName(String name);

    public abstract void checkOsNameOrThrow(String s);

    public abstract void showLicenseAlert(LicenseRequiredException ex);

    public abstract void init();

    public abstract Comp<?> overviewPage();

    public abstract boolean hasPaidLicense();

    public abstract boolean shouldReportError();

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            INSTANCE = ServiceLoader.load(layer, LicenseProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .findFirst()
                    .orElseThrow(() -> ExtensionException.corrupt("Missing license provider"));
        }
    }
}
