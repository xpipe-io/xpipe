package io.xpipe.app.util;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class LicenseRequiredException extends RuntimeException {

    String featureName;
    boolean plural;
    LicenseType minLicense;

    public LicenseRequiredException(String featureName, boolean plural, LicenseType minLicense) {
        super(featureName + " are only supported with a " + minLicense.name().toLowerCase() + " license");
        this.featureName = featureName;
        this.plural = plural;
        this.minLicense = minLicense;
    }
}
