package io.xpipe.app.util;

import lombok.Getter;

public class LicenseRequiredException extends RuntimeException {

    @Getter
    private final LicensedFeature feature;

    public LicenseRequiredException(LicensedFeature feature) {
        super(feature.getDisplayName() + (feature.isPlural() ? " are" : " is") + " only supported with a professional license");
        this.feature = feature;
    }


    public LicenseRequiredException(String featureName, boolean plural, LicensedFeature feature) {
        super(featureName + (plural ? " are" : " is") + " only supported with a professional license");
        this.feature = feature;
    }
}
