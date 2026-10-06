package io.xpipe.ext.base.script;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.ext.DataStorageExtensionProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;

public class ScriptValidDataStorageProvider extends DataStorageExtensionProvider {

    @Override
    public void storageInit() {
        for (DataStoreEntry e : DataStorage.get().getStoreEntries()) {
            if (e.getStore() instanceof ScriptStore ss && ss.getState().isEnabled() && e.getValidity().isUsable()) {
                try {
                    ss.getTextSource().checkAvailable();
                } catch (Exception ex) {
                    ErrorEventFactory.fromThrowable(ex).omit().handle();
                    ss.disable();
                }
            }
        }
    }
}
