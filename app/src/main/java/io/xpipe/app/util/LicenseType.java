package io.xpipe.app.util;

public enum LicenseType {

    COMMUNITY,
    PROFESSIONAL,
    ENTERPRISE;

    public static boolean isAtLeast(LicenseType input, LicenseType target) {
        if (target == COMMUNITY) {
            return true;
        }

        if (target == PROFESSIONAL) {
            return input == PROFESSIONAL || input == ENTERPRISE;
        }

        return target == ENTERPRISE;
    }
}
