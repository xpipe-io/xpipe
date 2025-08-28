package io.xpipe.app.util;

import io.xpipe.app.comp.base.ChoicePaneComp;
import io.xpipe.app.core.AppI18n;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Region;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

@Builder
public class OptionsChoiceBuilder {

    private final Property<?> property;
    private final List<Class<?>> available;
    private final List<Class<?>> selectable;
    private final Function<ComboBox<ChoicePaneComp.Entry>, Region> transformer;
    private final boolean allowNull;
    private final Object customConfiguration;

    @SneakyThrows
    private static String createIdForClass(Class<?> c) {
        var custom = Arrays.stream(c.getDeclaredMethods())
                .filter(m -> m.getName().equals("getOptionsNameKey"))
                .findFirst();
        if (custom.isPresent()) {
            return (String) custom.get().invoke(null);
        }

        var a = c.getAnnotation(JsonTypeName.class);
        if (a != null) {
            return a.value();
        }

        return null;
    }

    private static Method findCreateOptionsMethod(Class<?> c) {
        return Arrays.stream(c.getDeclaredMethods())
                .filter(method -> method.getName().equals("createOptions"))
                .findFirst()
                .orElse(null);
    }

    private static OptionsBuilder createOptionsForClass(
            Class<?> c, Property<Object> property, Object customConfiguration) {
        var method = findCreateOptionsMethod(c);
        if (method == null) {
            return null;
        }

        try {
            method.setAccessible(true);
            var r = method.getParameters().length == 2
                    ? method.invoke(null, property, customConfiguration)
                    : method.invoke(null, property);
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
        } catch (Exception ignored) {
        }

        try {
            var bm = c.getDeclaredMethod("builder");
            bm.setAccessible(true);
            var b = bm.invoke(null);
            var m = b.getClass().getDeclaredMethod("build");
            m.setAccessible(true);
            var defValue = c.cast(m.invoke(b));
            return defValue;
        } catch (Exception ignored) {
        }

        try {
            var defConstructor = c.getDeclaredConstructor();
            var defValue = defConstructor.newInstance();
            return defValue;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> OptionsBuilder build() {
        Property<T> s = (Property<T>) property;
        var initial = s.getValue();
        var sub = available;
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
        for (Class<?> aClass : sub) {
            var compatible = aClass.isInstance(s.getValue());
            properties.add(
                    new SimpleObjectProperty<>(compatible ? s.getValue() : createDefaultInstanceForClass(aClass)));
        }

        property.addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            for (int i = 0; i < sub.size(); i++) {
                var c = sub.get(i);
                if (c.isAssignableFrom(newValue.getClass())) {
                    properties.get(i + (allowNull ? 1 : 0)).setValue(newValue);
                }
            }
        });

        var map = new LinkedHashMap<ObservableValue<String>, OptionsBuilder>();
        for (int i = 0; i < sub.size(); i++) {
            map.put(
                    AppI18n.observable(createIdForClass(sub.get(i))),
                    createOptionsForClass(sub.get(i), properties.get(i + (allowNull ? 1 : 0)), customConfiguration));
        }
        if (allowNull) {
            var key = AppI18n.observable("none");
            if (map.containsKey(key)) {
                map.putFirst(AppI18n.observable("empty"), new OptionsBuilder());
            } else {
                map.putFirst(key, new OptionsBuilder());
            }
        }

        return new OptionsBuilder()
                .choice(selected, map, transformer)
                .bindChoice(
                        () -> {
                            if (selected.get() == -1) {
                                return new ReadOnlyObjectWrapper<>(initial);
                            }

                            var prop = properties.get(selected.get());
                            return (Property<? extends T>) prop;
                        },
                        s);
    }
}
