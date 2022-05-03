package io.xpipe.extension;

import io.xpipe.core.source.DataSourceDescriptor;
import io.xpipe.core.source.DataSourceInfo;
import io.xpipe.core.store.DataStore;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public interface UniformDataSourceProvider extends DataSourceProvider {

    @Override
    default ConfigProvider getConfigProvider() {
        return ConfigProvider.empty(List.of(getId()), this::createDefaultDescriptor);
    }

    @Override
    default DataSourceDescriptor<?> createDefaultDescriptor() {
        try {
            return (DataSourceDescriptor<?>) getDescriptorClass().getDeclaredConstructors()[0].newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    default DataSourceDescriptor<?> createDefaultDescriptor(DataStore input) throws Exception {
        return createDefaultDescriptor();
    }

    @Override
    default DataSourceDescriptor<?> createDefaultWriteDescriptor(DataStore input, DataSourceInfo info) throws Exception {
        return createDefaultDescriptor();
    }
}
