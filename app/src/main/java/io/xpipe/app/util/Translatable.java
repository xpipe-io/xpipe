package io.xpipe.app.util;

import javafx.beans.value.ObservableValue;

public interface Translatable {

    ObservableValue<String> toTranslatedString();
}
