package io.xpipe.ext.json;

import io.xpipe.core.source.DataSourceDescriptor;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.SimpleFileDataSourceProvider;
import io.xpipe.extension.UniformDataSourceProvider;

import java.util.LinkedHashMap;
import java.util.Map;

public class MyRawFileProvider implements UniformDataSourceProvider, SimpleFileDataSourceProvider, DataSourceProvider {

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.RAW;
    }

    @Override
    public Map<String, String> getSupportedExtensions() {
        var map = new LinkedHashMap<String, String>();
        map.put(i18nKey("fileName"), "myf");
        return map;
    }

    @Override
    public Class<? extends DataSourceDescriptor<?>> getDescriptorClass() {
        return MyRawFileDescriptor.class;
    }
}
