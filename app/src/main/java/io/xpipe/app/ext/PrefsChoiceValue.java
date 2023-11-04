package io.xpipe.app.ext;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.Translatable;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.List;

public interface PrefsChoiceValue extends Translatable {

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

    @SuppressWarnings("unchecked")
    static <T> List<T> getSupported(Class<T> type) {
        try {
            return (List<T>) type.getDeclaredField("SUPPORTED").get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            var all = getAll(type);
            if (all == null) {
                throw new AssertionError();
            }

            return all.stream().filter(t -> ((PrefsChoiceValue) t).isSelectable()).toList();
        }
    }

    default boolean isAvailable() {
        return true;
    }

    default boolean isSelectable() {
        return true;
    }

    @Override
    default String toTranslatedString() {
        return AppI18n.get(getId());
    }

    String getId();
}
