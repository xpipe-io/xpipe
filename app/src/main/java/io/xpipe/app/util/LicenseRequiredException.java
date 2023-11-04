package io.xpipe.app.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(
        makeFinal = true,
        level = AccessLevel.PRIVATE)
public class LicenseRequiredException extends RuntimeException {

    String featureName;
    boolean plural;

    public LicenseRequiredException(String featureName, boolean plural) {
        super(featureName + (plural ? " are" : " is") + " only supported with a professional license");
        this.featureName = featureName;
        this.plural = plural;
    }
}
