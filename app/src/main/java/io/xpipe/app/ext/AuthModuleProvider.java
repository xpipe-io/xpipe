package io.xpipe.app.ext;

import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.secret.DataStorageAccessHandler;
import io.xpipe.app.util.SecretValue;
import io.xpipe.app.util.WindowsCredential;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

public abstract class AuthModuleProvider {

    private static AuthModuleProvider INSTANCE;

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            INSTANCE = ServiceLoader.load(layer, AuthModuleProvider.class).stream()
                    .map(p -> p.get())
                    .findFirst()
                    .orElseThrow();
        }

        @Override
        public boolean initForCli() {
            return false;
        }
    }

    public static AuthModuleProvider get() {
        return INSTANCE;
    }

    public abstract DataStorageAccessHandler getStorageAccessHandler();

    public abstract OptionsBuilder createVaultAccessOptions();

    public abstract List<Class<?>> getPasswordManagerClasses();

    public abstract List<Class<?>> getSshIdentityStrategyClasses();

    public abstract List<Class<?>> getSshShortLivedCertificateImplClasses();

    public abstract Optional<WindowsCredential> getWindowsCredential(String target, int type);

    public abstract void setWindowsCredential(String target, int type, int persist, String userName, SecretValue password);
}
