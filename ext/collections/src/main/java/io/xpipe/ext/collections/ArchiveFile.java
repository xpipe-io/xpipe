package io.xpipe.ext.collections;

import io.xpipe.core.source.CollectionDataSource;
import io.xpipe.core.source.CollectionReadConnection;
import io.xpipe.core.source.CollectionWriteConnection;
import io.xpipe.core.source.WriteMode;
import io.xpipe.core.store.StreamDataStore;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class ArchiveFile extends CollectionDataSource<StreamDataStore> {

    protected abstract String getId();

    @Override
    public CollectionWriteConnection newWriteConnection(WriteMode mode) {
        return null;
    }

    @Override
    public CollectionReadConnection newReadConnection() {
        return new ArchiveReadConnection(getId(), store);
    }
}
