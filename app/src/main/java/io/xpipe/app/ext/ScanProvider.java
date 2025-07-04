package io.xpipe.app.ext;

import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.ModuleLayerLoader;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public abstract class ScanProvider {

    private static List<ScanProvider> ALL;

    public static List<ScanProvider> getAll() {
        return ALL;
    }

    public ScanOpportunity create(DataStoreEntry entry, ShellControl sc) throws Exception {
        return null;
    }

    public abstract void scan(DataStoreEntry entry, ShellControl sc) throws Throwable;

    @Value
    @AllArgsConstructor
    public class ScanOpportunity {
        String nameKey;
        boolean disabled;
        String licenseFeatureId;

        public ScanOpportunity(String nameKey, boolean disabled) {
            this.nameKey = nameKey;
            this.disabled = disabled;
            this.licenseFeatureId = null;
        }

        public String getLicensedFeatureId() {
            return licenseFeatureId;
        }

        public ScanProvider getProvider() {
            return ScanProvider.this;
        }
    }

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ALL = ServiceLoader.load(layer, ScanProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .sorted(Comparator.comparing(
                            scanProvider -> scanProvider.getClass().getName()))
                    .collect(Collectors.toList());
        }
    }
}
