package io.xpipe.core.util;

import io.xpipe.core.process.ShellControl;

import java.util.Optional;
import java.util.ServiceLoader;

public abstract class ProxyManagerProvider {

    private static ProxyManagerProvider INSTANCE;

    public static ProxyManagerProvider get() {
        if (INSTANCE == null) {
            INSTANCE = ServiceLoader.load(ModuleLayer.boot(), ProxyManagerProvider.class)
                    .findFirst()
                    .orElseThrow();
        }

        return INSTANCE;
    }

    public abstract Optional<String> checkCompatibility(ShellControl pc);

    public abstract boolean setup(ShellControl pc);
}
