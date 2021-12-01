package io.xpipe.api;

import io.xpipe.core.source.DataSourceId;

public abstract class XPipeDataTableBuilder {

    private DataSourceId id;

    public abstract void write();

    public abstract void commit();
}
