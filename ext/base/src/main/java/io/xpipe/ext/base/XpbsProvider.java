package io.xpipe.ext.base;

import io.xpipe.app.util.UniformDataSourceProvider;
import io.xpipe.core.impl.XpbsSource;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;

import java.util.List;
import java.util.Map;

public class XpbsProvider implements SimpleFileDataSourceProvider<XpbsSource>, UniformDataSourceProvider<XpbsSource> {

    @Override
    public XpbsSource createDefaultSource(DataStore input) {
        return XpbsSource.builder().store(input.asNeeded()).build();
    }

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.STRUCTURE;
    }

    @Override
    public Class<XpbsSource> getSourceClass() {
        return XpbsSource.class;
    }

    @Override
    public Map<String, List<String>> getSupportedExtensions() {
        return Map.of(i18nKey("fileName"), List.of("xpbs"));
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("xpbs");
    }

    @Override
    public boolean shouldShow(DataSourceType type) {
        return false;
    }
}
