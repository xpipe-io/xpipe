package io.xpipe.ext.base;

import io.xpipe.core.impl.InMemoryStore;
import io.xpipe.core.source.CollectionDataSource;
import io.xpipe.core.source.CollectionWriteConnection;
import io.xpipe.core.source.WriteMode;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class ReadOnlyCollectionSource extends CollectionDataSource<InMemoryStore> {

    @Override
    protected CollectionWriteConnection newWriteConnection(WriteMode mode) {
        throw new UnsupportedOperationException();
    }
}
