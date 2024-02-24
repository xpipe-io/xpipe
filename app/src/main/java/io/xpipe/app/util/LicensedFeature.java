package io.xpipe.app.util;

import java.util.Optional;

public interface LicensedFeature {

    default Optional<String> getDescriptionSuffix() {
        if (isSupported()) {
            return Optional.empty();
        }

        if (isPreviewSupported()) {
            return Optional.of("Preview");
        }

        return Optional.of("Pro");
    }

    String getId();

    String getDisplayName();

    boolean isPlural();

    boolean isSupported();

    boolean isPreviewSupported();

    default void throwIfUnsupported() throws LicenseRequiredException {
        if (!isSupported()) {
            throw new LicenseRequiredException(this);
        }
    }
}
