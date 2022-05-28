package io.xpipe.extension;

import io.xpipe.core.source.DataSource;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FileDataStore;
import io.xpipe.core.store.StreamDataStore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface SimpleFileDataSourceProvider<T extends DataSource<?>> extends DataSourceProvider<T> {

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

                if (store instanceof FileDataStore l) {
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
