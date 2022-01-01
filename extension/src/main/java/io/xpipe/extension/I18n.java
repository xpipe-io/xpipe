package io.xpipe.extension;

import java.util.ServiceLoader;
import java.util.function.Supplier;

public interface I18n {

    I18n INSTANCE = ServiceLoader.load(I18n.class).findFirst().orElseThrow();

    public static Supplier<String> resolver(String s, Object... vars) {
        return () -> get(s, vars);
    }

    public static String get(String s, Object... vars) {
        return INSTANCE.getLocalised(s, vars);
    }

    String getLocalised(String s, Object... vars);
}
