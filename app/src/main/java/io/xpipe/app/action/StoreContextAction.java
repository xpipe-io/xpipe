package io.xpipe.app.action;

import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;

import java.util.List;

public interface StoreContextAction {

    List<DataStoreEntry> getStoreEntryContext();
}
