package io.xpipe.app.ext;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.Translatable;

import javafx.beans.value.ObservableValue;

import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.List;

public interface PrefsChoiceValue extends PrefsValue, Translatable {

    @SuppressWarnings("unchecked")
    @SneakyThrows
    static <T> List<T> getAll(Class<T> type) {
        if (Enum.class.isAssignableFrom(type)) {
            return Arrays.asList(type.getEnumConstants());
        }

        try {
            type.getDeclaredField("ALL");
        } catch (NoSuchFieldException e) {
            return null;
        }

        try {
            return (List<T>) type.getDeclaredField("ALL").get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return List.of(type.getEnumConstants());
        }
    }

    static <T> List<T> getSupported(Class<T> type) {
        var all = getAll(type);
        if (all == null) {
            throw new AssertionError();
        }

        return all.stream().filter(t -> ((PrefsChoiceValue) t).isSelectable()).toList();
    }

    @Override
    default ObservableValue<String> toTranslatedString() {
        return AppI18n.observable(getId());
    }

    @SuppressWarnings("unused")
    String getId();
}
