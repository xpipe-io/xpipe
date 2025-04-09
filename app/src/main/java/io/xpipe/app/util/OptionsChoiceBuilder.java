package io.xpipe.app.util;

import io.xpipe.app.comp.base.ChoicePaneComp;
import io.xpipe.app.core.AppI18n;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Region;

import com.fasterxml.jackson.annotation.JsonTypeName;
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

    private static OptionsBuilder createOptionsForClass(Class<?> c, Property<Object> property) {
        try {
            var method = c.getDeclaredMethod("createOptions", Property.class);
            method.setAccessible(true);
            var r = method.invoke(null, property);
            if (r != null) {
                return (OptionsBuilder) r;
            }
        } catch (Exception ignored) {
        }
        return new OptionsBuilder();
    }

    private static Object createDefaultInstanceForClass(Class<?> c) {
        try {
            var cd = c.getDeclaredMethod("createDefault");
            cd.setAccessible(true);
            var defValue = cd.invoke(null);
            return defValue;
        } catch (Exception e) {
        }

        try {
            var bm = c.getDeclaredMethod("builder");
            bm.setAccessible(true);
            var b = bm.invoke(null);
            var m = b.getClass().getDeclaredMethod("build");
            m.setAccessible(true);
            var defValue = c.cast(m.invoke(b));
            return defValue;
        } catch (Exception e) {
        }

        try {
            var defConstructor = c.getDeclaredConstructor();
            var defValue = defConstructor.newInstance();
            return defValue;
        } catch (Exception e) {
            return null;
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
        var selectedIndex = s.getValue() == null
                ? (allowNull ? 0 : -1)
                : sub.stream()
                        .filter(c -> c.equals(s.getValue().getClass()))
                        .findFirst()
                        .map(c -> sub.indexOf(c) + (allowNull ? 1 : 0))
                        .orElse(-1);
        var selected = new SimpleIntegerProperty(selectedIndex);

        var properties = new ArrayList<Property<Object>>();
        if (allowNull) {
            properties.add(new SimpleObjectProperty<>());
        }
        for (int i = 0; i < sub.size(); i++) {
            var compatible = sub.get(i).isInstance(s.getValue());
            properties.add(
                    new SimpleObjectProperty<>(compatible ? s.getValue() : createDefaultInstanceForClass(sub.get(i))));
        }

        property.addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            for (int i = 0; i < subclasses.size(); i++) {
                var c = subclasses.get(i);
                if (c.isAssignableFrom(newValue.getClass())) {
                    properties.get(i + (allowNull ? 1 : 0)).setValue(newValue);
                }
            }
        });

        var map = new LinkedHashMap<ObservableValue<String>, OptionsBuilder>();
        if (allowNull) {
            map.put(AppI18n.observable("none"), new OptionsBuilder());
        }
        for (int i = 0; i < sub.size(); i++) {
            map.put(
                    AppI18n.observable(createIdForClass(sub.get(i))),
                    createOptionsForClass(sub.get(i), properties.get(i + (allowNull ? 1 : 0))));
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
