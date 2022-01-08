package io.xpipe.extension;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;

import java.util.ServiceLoader;
import java.util.function.Supplier;

public interface I18n {

    I18n INSTANCE = ServiceLoader.load(I18n.class).findFirst().orElseThrow();

    public static Supplier<String> resolver(String s, Object... vars) {
        return () -> get(s, vars);
    }

    public static ObservableValue<String> observable(String s, Object... vars) {
        return Bindings.createStringBinding(() -> {
            return get(s, vars);
        });
    }

    public static String get(String s, Object... vars) {
        return INSTANCE.getLocalised(s, vars);
    }

    String getLocalised(String s, Object... vars);
}
