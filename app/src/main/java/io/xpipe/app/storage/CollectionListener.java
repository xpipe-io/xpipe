package io.xpipe.app.storage;

public interface CollectionListener {

    void onUpdate();

    void onEntryAdd(DataSourceEntry entry);

    void onEntryRemove(DataSourceEntry entry);
}
