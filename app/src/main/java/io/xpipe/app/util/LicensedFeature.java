package io.xpipe.app.util;

public interface LicensedFeature {

    String getId();

    String getDisplayName();

    boolean isPlural();

    boolean isSupported();

    boolean isPreviewSupported();

    void throwIfUnsupported() throws LicenseRequiredException;
}
