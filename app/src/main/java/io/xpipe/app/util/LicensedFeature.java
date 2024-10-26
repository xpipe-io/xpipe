package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;

import javafx.beans.value.ObservableValue;

import java.util.Optional;

public interface LicensedFeature {

    Optional<String> getDescriptionSuffix();

    public default ObservableValue<String> suffixObservable(ObservableValue<String> s) {
        return s.map(s2 ->
                getDescriptionSuffix().map(suffix -> s2 + " (" + suffix + ")").orElse(""));
    }

    public default ObservableValue<String> suffixObservable(String key) {
        return AppI18n.observable(key).map(s -> getDescriptionSuffix()
                .map(suffix -> s + " (" + suffix + ")")
                .orElse(""));
    }

    public default String suffix(String s) {
        return getDescriptionSuffix().map(suffix -> s + " (" + suffix + ")").orElse(s);
    }

    String getId();

    String getDisplayName();

    boolean isPlural();

    boolean isSupported();

    boolean isPreviewSupported();

    void throwIfUnsupported() throws LicenseRequiredException;
}
