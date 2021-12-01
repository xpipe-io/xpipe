package io.xpipe.core.source;

public abstract class DataStructureSource implements DataSource {

    public abstract DataSourceConnection openConnection() throws Exception;

    @Override
    public DataSourceType getType() {
        return DataSourceType.STRUCTURE;
    }
}
