package io.xpipe.app.prefs;

import io.xpipe.app.core.AppI18n;
import javafx.beans.value.ObservableValue;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PrefsCapability {

    public static PrefsCapability of(String nameKey, String descriptionKey, Type type) {
        return PrefsCapability.builder().name(AppI18n.observable(nameKey)).description(AppI18n.observable(descriptionKey)).type(type).build();
    }

    public static PrefsCapability of(String nameKey, Type type) {
        return PrefsCapability.builder().name(AppI18n.observable(nameKey)).description(AppI18n.observable(nameKey + "Description")).type(type).build();
    }

    public enum Type {

        SUPPORTED,
        UNSUPPORTED,
        WARNING;

        public static Type of(boolean b) {
            return b ? SUPPORTED : UNSUPPORTED;
        }
    }

    ObservableValue<String> name;
    ObservableValue<String> description;
    Type type;
}
