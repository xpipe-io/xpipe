package io.xpipe.ext.json;

import io.xpipe.core.source.RawDataSourceDescriptor;
import io.xpipe.core.source.RawReadConnection;
import io.xpipe.core.source.RawWriteConnection;
import io.xpipe.core.store.StreamDataStore;

public class MyRawFileDescriptor extends RawDataSourceDescriptor<StreamDataStore> {
    @Override
    protected RawWriteConnection newWriteConnection(StreamDataStore store) {
        return new MyRawFileWriteConnection(store);
    }

    @Override
    protected RawReadConnection newReadConnection(StreamDataStore store) {
        return new MyRawFileReadConnection(store);
    }
}
