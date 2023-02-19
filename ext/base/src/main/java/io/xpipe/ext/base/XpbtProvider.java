package io.xpipe.ext.base;

import io.xpipe.app.util.UniformDataSourceProvider;
import io.xpipe.core.impl.XpbtSource;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;

import java.util.List;
import java.util.Map;

public class XpbtProvider implements SimpleFileDataSourceProvider<XpbtSource>, UniformDataSourceProvider<XpbtSource> {

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.TABLE;
    }

    @Override
    public XpbtSource createDefaultSource(DataStore input) throws Exception {
        return XpbtSource.builder().store(input.asNeeded()).build();
    }

    @Override
    public Class<XpbtSource> getSourceClass() {
        return XpbtSource.class;
    }

    @Override
    public Map<String, List<String>> getSupportedExtensions() {
        return Map.of(i18nKey("fileName"), List.of("xpbt"));
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("xpbt");
    }

    @Override
    public boolean shouldShow(DataSourceType type) {
        return false;
    }
}
