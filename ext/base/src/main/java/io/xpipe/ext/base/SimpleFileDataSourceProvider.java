package io.xpipe.ext.base;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.app.ext.DataSourceProviders;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FilenameStore;
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
    default boolean prefersStore(DataStore store, DataSourceType type) {
        if (type != null && type != getPrimaryType()) {
            return false;
        }

        for (var e : getSupportedExtensions().entrySet()) {
            if (e.getValue() == null) {
                continue;
            }

            for (var ext : e.getValue()) {
                if (ext == null) {
                    continue;
                }

                if (store instanceof FilenameStore l) {
                    return l.getFileExtension().equalsIgnoreCase(ext);
                }
            }
        }

        if (store instanceof HttpStore s) {
            var info = s.getInfo();
            if (info.getMimeType() != null) {
                return getSupportedMimeTypes().contains(info.getMimeType());
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

    public default List<String> getSupportedMimeTypes() {
        return List.of();
    }

    @Override
    default FileProvider getFileProvider() {
        return new FileProvider() {
            @Override
            public String getFileName() {
                return AppI18n.get(getNameI18nKey());
            }

            @Override
            public Map<String, List<String>> getFileExtensions() {
                var map = new LinkedHashMap<>(getSupportedExtensions());
                return map;
            }
        };
    }
}
