package io.xpipe.app.ext;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.Translatable;
import javafx.beans.value.ObservableValue;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.List;

public interface PrefsValue {

    default boolean isAvailable() {
        return true;
    }

    default boolean isSelectable() {
        return true;
    }
}
