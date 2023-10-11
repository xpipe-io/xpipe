package io.xpipe.app.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class LicenseRequiredException extends RuntimeException {

    String featureName;
    boolean plural;
    LicenseType minLicense;

    public LicenseRequiredException(String featureName, boolean plural, LicenseType minLicense) {
        super(featureName + (plural ? " are" : " is") + " only supported with a " + minLicense.name().toLowerCase() + " license");
        this.featureName = featureName;
        this.plural = plural;
        this.minLicense = minLicense;
    }
}
