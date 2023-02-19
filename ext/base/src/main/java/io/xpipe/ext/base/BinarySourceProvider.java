package io.xpipe.ext.base;

import io.xpipe.app.util.UniformDataSourceProvider;
import io.xpipe.core.impl.BinarySource;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BinarySourceProvider
        implements UniformDataSourceProvider<BinarySource>, SimpleFileDataSourceProvider<BinarySource> {

    @Override
    public BinarySource createDefaultSource(DataStore input) throws Exception {
        return BinarySource.builder().store(input.asNeeded()).build();
    }

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.RAW;
    }

    @Override
    public Class<BinarySource> getSourceClass() {
        return BinarySource.class;
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("binary", "bin", "bytes");
    }

    @Override
    public Map<String, List<String>> getSupportedExtensions() {
        var map = new LinkedHashMap<String, List<String>>();
        map.put(i18nKey("anyBinaryFile"), null);
        map.put(i18nKey("dataFile"), List.of("dat"));
        map.put(i18nKey("binaryFile"), List.of("bin"));
        return map;
    }
}
