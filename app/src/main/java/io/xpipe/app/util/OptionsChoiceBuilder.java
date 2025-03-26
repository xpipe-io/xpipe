package io.xpipe.app.util;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ChoicePaneComp;
import io.xpipe.app.core.AppI18n;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Region;
import lombok.Builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

@Builder
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
            return r != null ? (OptionsBuilder) r : new OptionsBuilder();
        } catch (Exception e) {
            return new OptionsBuilder();
        }
    }

    private final Property<?> property;
    private final List<Class<?>> subclasses;
    private final Function<ComboBox<ChoicePaneComp.Entry>, Region> transformer;
    private final boolean allowNull;

    @SuppressWarnings("unchecked")
    public <T> OptionsBuilder build() {
        Property<T> s = (Property<T>) property;
        var sub = subclasses;
        var selectedIndex = s.getValue() == null ? (allowNull ? 0 : -1) : sub.stream().filter(c -> c.equals(s.getValue().getClass()))
                .findFirst().map(c -> sub.indexOf(c))
                .orElse(-1);
        var selected = new SimpleIntegerProperty(selectedIndex);

        var properties = new ArrayList<Property<Object>>();
        if (allowNull) {
            properties.add(new SimpleObjectProperty<>());
        }
        for (int i = 0; i < sub.size(); i++) {
            properties.add(new SimpleObjectProperty<>(selectedIndex == i ? s.getValue() : null));
        }

        property.addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            for (int i = 0; i < subclasses.size(); i++) {
                var c = subclasses.get(i);
                if (c.isAssignableFrom(newValue.getClass())) {
                    properties.get(i).setValue(newValue);
                }
            }
        });

        var map = new LinkedHashMap<ObservableValue<String>, OptionsBuilder>();
        if (allowNull) {
            map.put(AppI18n.observable("none"), new OptionsBuilder());
        }
        for (int i = 0; i < sub.size(); i++) {
            map.put(AppI18n.observable(createIdForClass(sub.get(i))), createOptionsForClass(sub.get(i), properties.get(i)));
        }

        return new OptionsBuilder()
                .choice(selected, map, transformer)
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
