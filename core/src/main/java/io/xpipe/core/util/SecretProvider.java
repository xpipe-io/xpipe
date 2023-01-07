package io.xpipe.core.util;

import java.util.ServiceLoader;

public abstract class SecretProvider {

    private static final SecretProvider INSTANCE = ServiceLoader.load(ModuleLayer.boot(), SecretProvider.class).findFirst().orElseThrow();

    public abstract byte[] encrypt(byte[] c);

    public abstract byte[] decrypt(byte[] c);

    public static SecretProvider get() {
        return INSTANCE;
    }
}
