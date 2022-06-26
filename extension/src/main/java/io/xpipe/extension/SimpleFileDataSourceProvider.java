package io.xpipe.extension;

import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FileStore;
import io.xpipe.core.store.StreamDataStore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface SimpleFileDataSourceProvider<T extends DataSource<?>> extends DataSourceProvider<T> {

    @Override
    default boolean supportsConversion(T in, DataSourceType t) {
        return t == DataSourceType.RAW;
    }

    @Override
    default DataSource<?> convert(T in, DataSourceType t) throws Exception {
        return DataSourceProviders.byId("binary").createDefaultSource(in.getStore());
    }

    @Override
    default boolean prefersStore(DataStore store) {
        for (var e : getSupportedExtensions().entrySet()) {
            if (e.getValue() == null) {
                continue;
            }

            for (var ext : e.getValue()) {
                if (ext == null) {
                    continue;
                }

                if (store instanceof FileStore l) {
                    return l.getFileName().endsWith("." + ext);
                }
            }
        }
        return false;
    }

    @Override
    default boolean couldSupportStore(DataStore store) {
        return store instanceof StreamDataStore;
    }

    default String getNameI18nKey() {
        return i18nKey("displayName");
    }
    Map<String, List<String>> getSupportedExtensions();

    @Override
    default FileProvider getFileProvider() {
        return new FileProvider() {
            @Override
            public String getFileName() {
                return I18n.get(getNameI18nKey());
            }

            @Override
            public Map<String, List<String>> getFileExtensions() {
                var map = new LinkedHashMap<>(getSupportedExtensions());
                return map;
            }
        };
    }
}
