package io.xpipe.app.ext;

import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.ModuleLayerLoader;
import lombok.Value;
import org.apache.commons.lang3.function.FailableRunnable;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public abstract class ScanProvider {

    @Value
    public static class ScanOperation {
        String nameKey;
        FailableRunnable<Exception> scanner;
    }

    private static List<ScanProvider> ALL;

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ALL = ServiceLoader.load(layer, ScanProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .sorted(Comparator.comparing(scanProvider -> scanProvider.getClass().getName()))
                    .collect(Collectors.toList());
        }

        @Override
        public boolean requiresFullDaemon() {
            return true;
        }

        @Override
        public boolean prioritizeLoading() {
            return false;
        }
    }

    public static List<ScanProvider> getAll() {
        return ALL;
    }

    public  abstract ScanOperation create(DataStore store);
}
