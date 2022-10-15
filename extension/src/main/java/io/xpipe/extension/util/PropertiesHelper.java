package io.xpipe.extension.util;

import javafx.beans.property.Property;

import java.util.Map;

public class PropertiesHelper {

    public static <T, V> void bindExclusive(
            Property<V> selected, Map<V, ? extends Property<T>> map, Property<T> toBind) {
        selected.addListener((c, o, n) -> {
            toBind.unbind();
            toBind.bind(map.get(n));
        });

        toBind.bind(map.get(selected.getValue()));
    }
}
