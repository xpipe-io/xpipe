package io.xpipe.extension;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;

import java.util.ServiceLoader;

public interface I18n {

    I18n INSTANCE = ServiceLoader.load(I18n.class).findFirst().orElseThrow();


    public static ObservableValue<String> observable(String s, Object... vars) {
        if (s == null) {
            return null;
        }

        var key = INSTANCE.getKey(s);
        return Bindings.createStringBinding(() -> {
            return get(key, vars);
        });
    }

    public static String get(String s, Object... vars) {
        return INSTANCE.getLocalised(s, vars);
    }

    String getKey(String s);

    String getLocalised(String s, Object... vars);
}
