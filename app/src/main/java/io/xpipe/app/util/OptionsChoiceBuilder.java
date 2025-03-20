package io.xpipe.app.util;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.PasswordManager;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

public class OptionsChoiceBuilder {

    private static String createIdForClass(Class<?> c) {
        if (c.getAnnotation(JsonTypeName.class) != null) {
            var a = c.getAnnotation(JsonTypeName.class);
            return a.value();
        }

        return null;
    }

    private static OptionsBuilder createOptionsForClass(Class<?> c, Property<?> property) {
        try {
            var method = c.getDeclaredMethod("createOptions", Property.class);
            method.setAccessible(true);
            var r = method.invoke(null, property);
            return (OptionsBuilder) r;
        } catch (Exception e) {
            return new OptionsBuilder();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> OptionsBuilder comp(Property<T> s, List<Class<?>> sub) {
        var selectedIndex = s.getValue() == null ? -1 : sub.stream().filter(c -> c.equals(s.getValue().getClass()))
                .findFirst().map(c -> sub.indexOf(c))
                .orElse(0);
        var selected = new SimpleIntegerProperty(selectedIndex);

        var properties = new ArrayList<Property<?>>();
        for (int i = 0; i < sub.size(); i++) {
            properties.add(new SimpleObjectProperty<>(selectedIndex == i ? s.getValue() : null));
        }

        var map = new LinkedHashMap<ObservableValue<String>, OptionsBuilder>();
        for (int i = 0; i < sub.size(); i++) {
            map.put(AppI18n.observable(createIdForClass(sub.get(i))), createOptionsForClass(sub.get(i), properties.get(i)));
        }

        return new OptionsBuilder()
                .choice(selected, map)
                .bindChoice(
                        () -> {
                            if (selected.get() == -1) {
                                return new SimpleObjectProperty<>();
                            }

                            var prop = properties.get(selected.get());
                            return (Property<? extends T>) prop;
                        },
                        s);
    }

}
