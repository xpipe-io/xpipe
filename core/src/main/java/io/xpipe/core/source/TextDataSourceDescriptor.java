package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

public class TextDataSourceDescriptor<DS extends DataStore> implements DataSourceDescriptor<DS>  {

    @Override
    public DataSourceInfo determineInfo(DS store) throws Exception {
        return null;
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.TEXT;
    }
}
