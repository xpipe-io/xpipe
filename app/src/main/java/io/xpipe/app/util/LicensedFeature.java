package io.xpipe.app.util;

import javafx.beans.value.ObservableValue;

import java.util.Optional;

public interface LicensedFeature {

    Optional<String> getDescriptionSuffix();

    ObservableValue<String> suffixObservable(ObservableValue<String> s);

    default String suffix(String s) {
        return getDescriptionSuffix().map(suffix -> s + " (" + suffix + ")").orElse(s);
    }

    String getId();

    String getDisplayName();

    boolean isPlural();

    boolean isSupported();

    void throwIfUnsupported() throws LicenseRequiredException;
}
