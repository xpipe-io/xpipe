package io.xpipe.app.ext;

import io.xpipe.core.util.ModuleLayerLoader;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public abstract class DataStorageExtensionProvider {

    private static List<DataStorageExtensionProvider> ALL;

    public static List<DataStorageExtensionProvider> getAll() {
        return ALL;
    }

    public void storageInit() throws Exception {}

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ALL = ServiceLoader.load(layer, DataStorageExtensionProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .sorted(Comparator.comparing(
                            scanProvider -> scanProvider.getClass().getName()))
                    .collect(Collectors.toList());
        }
    }
}
