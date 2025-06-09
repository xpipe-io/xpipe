package io.xpipe.app.action;

import io.xpipe.app.storage.DataStoreEntry;

import java.util.List;

public interface StoreContextAction {

    List<DataStoreEntry> getStoreEntryContext();
}
