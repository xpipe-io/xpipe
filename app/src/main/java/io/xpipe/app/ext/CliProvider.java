package io.xpipe.app.ext;

import java.util.OptionalInt;
import java.util.ServiceLoader;

public abstract class CliProvider {

    private static CliProvider INSTANCE;

    public static void init(ModuleLayer layer) {
        INSTANCE = ServiceLoader.load(layer, CliProvider.class).stream()
                .map(p -> p.get())
                .findFirst()
                .orElse(null);
    }

    public static CliProvider get() {
        return INSTANCE;
    }

    public abstract int execute(String[] args);
}
