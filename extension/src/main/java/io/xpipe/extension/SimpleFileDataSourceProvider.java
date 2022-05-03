package io.xpipe.extension;

import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FileDataStore;
import io.xpipe.core.store.StreamDataStore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SimpleFileDataSourceProvider extends DataSourceProvider {

    @Override
    default Optional<String> determineDefaultName(DataStore store) {
        if (store instanceof FileDataStore l) {
            var n = l.getFileName();
            var i = n.lastIndexOf('.');
            return Optional.of(i != -1 ? n.substring(0, i) : n);
        }

        return Optional.empty();
    }

    @Override
    default boolean prefersStore(DataStore store) {
        for (var e : getSupportedExtensions().entrySet()) {
            if (e.getValue() == null) {
                continue;
            }

            if (store instanceof FileDataStore l) {
                return l.getFileName().matches("\\." + e.getValue() + "$");
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
