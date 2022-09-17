package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.xpipe.core.source.TableDataSource;
import io.xpipe.core.source.TableReadConnection;
import io.xpipe.core.source.TableWriteConnection;
import io.xpipe.core.store.StreamDataStore;

public class XpbtSource extends TableDataSource<StreamDataStore> {

    @JsonCreator
    public XpbtSource(StreamDataStore store) {
        super(store);
    }

    @Override
    protected TableWriteConnection newWriteConnection() {
        return new XpbtWriteConnection(store);
    }

    @Override
    protected TableReadConnection newReadConnection() {
        return new XpbtReadConnection(store);
    }
}
