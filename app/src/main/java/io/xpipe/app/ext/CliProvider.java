package io.xpipe.app.ext;

import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.ModuleLayerLoader;

import java.util.OptionalInt;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public abstract class CliProvider {

    private static CliProvider INSTANCE;

    public static CliProvider get() {
        return INSTANCE;
    }

    public abstract int execute(String[] args);

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            INSTANCE = ServiceLoader.load(layer, CliProvider.class).stream()
                    .map(p -> p.get())
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public boolean initForCli() {
            return true;
        }
    }
}
