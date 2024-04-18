package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;

import lombok.Getter;

@Getter
public class LicenseRequiredException extends RuntimeException {

    private final LicensedFeature feature;

    public LicenseRequiredException(LicensedFeature feature) {
        super(feature.getDisplayName() + " "
                + (feature.isPlural() ? AppI18n.get("areOnlySupported") : AppI18n.get("isOnlySupported")));
        this.feature = feature;
    }

    public LicenseRequiredException(String featureName, boolean plural, LicensedFeature feature) {
        super(featureName + " " + (plural ? AppI18n.get("areOnlySupported") : AppI18n.get("isOnlySupported")));
        this.feature = feature;
    }
}
