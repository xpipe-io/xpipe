package io.xpipe.app.hub.comp;

import io.xpipe.app.storage.DataStoreEntry;

public interface StoreCreationConsumer {

    void consume(DataStoreEntry entry, boolean validated);
}
