package io.xpipe.app.util;

import lombok.Getter;

@Getter
public class LicenseRequiredException extends RuntimeException {

    private final LicensedFeature feature;

    public LicenseRequiredException(LicensedFeature feature) {
        super(LicenseProvider.get().formatExceptionMessage(feature.getDisplayName(), feature.isPlural(), feature));
        this.feature = feature;
    }

    public LicenseRequiredException(String featureName, boolean plural, LicensedFeature feature) {
        super(LicenseProvider.get().formatExceptionMessage(featureName, plural, feature));
        this.feature = feature;
    }
}
