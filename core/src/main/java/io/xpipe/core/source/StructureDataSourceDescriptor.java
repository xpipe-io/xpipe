package io.xpipe.core.source;

public abstract class StructureDataSourceDescriptor implements DataSourceDescriptor {

    public abstract DataSourceConnection openConnection() throws Exception;

    @Override
    public DataSourceType getType() {
        return DataSourceType.STRUCTURE;
    }
}
