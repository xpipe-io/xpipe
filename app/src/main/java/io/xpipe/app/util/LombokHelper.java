package io.xpipe.app.util;

import lombok.SneakyThrows;

public class LombokHelper {

    @SneakyThrows
    public static <T> T build(Class<T> clazz) {
        var c = clazz.getDeclaredMethod("builder");
        c.setAccessible(true);
        var b = c.invoke(null);
        var m = b.getClass().getDeclaredMethod("build");
        m.setAccessible(true);
        return clazz.cast(m.invoke(b));
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <B, T> B toBuilder(T val) {
        var c = val.getClass().getDeclaredMethod("toBuilder");
        c.setAccessible(true);
        var b = c.invoke(val);
        return (B) b;
    }
}
