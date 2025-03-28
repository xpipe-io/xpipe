package io.xpipe.app.comp.store;

import io.xpipe.app.storage.DataStoreEntry;

public interface StoreCreationConsumer {

    void consume(DataStoreEntry entry, boolean validated);
}
