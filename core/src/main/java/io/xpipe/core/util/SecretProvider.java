package io.xpipe.core.util;

import java.util.ServiceLoader;

public abstract class SecretProvider {

    private static SecretProvider INSTANCE;

    public abstract byte[] encrypt(byte[] c);

    public abstract byte[] decrypt(byte[] c);

    public static SecretProvider get() {
        if (INSTANCE == null) {
            INSTANCE = ServiceLoader.load(ModuleLayer.boot(), SecretProvider.class).findFirst().orElseThrow();
        }

        return INSTANCE;
    }
}
