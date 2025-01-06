package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;

import javafx.beans.value.ObservableValue;

import java.util.Optional;

public interface LicensedFeature {

    Optional<String> getDescriptionSuffix();

    ObservableValue<String> suffixObservable(ObservableValue<String> s);

    default ObservableValue<String> suffixObservable(String key) {
        return suffixObservable(AppI18n.observable(key));
    }

    default String suffix(String s) {
        return getDescriptionSuffix().map(suffix -> s + " (" + suffix + "+)").orElse(s);
    }

    String getId();

    String getDisplayName();

    boolean isPlural();

    boolean isSupported();

    boolean isPreviewSupported();

    void throwIfUnsupported() throws LicenseRequiredException;
}
