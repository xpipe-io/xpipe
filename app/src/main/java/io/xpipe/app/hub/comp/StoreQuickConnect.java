package io.xpipe.app.hub.comp;

import io.xpipe.app.action.QuickConnectProvider;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.ext.*;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.ThreadHelper;

import java.util.*;

public class StoreQuickConnect {

    public static final UUID STORE_ID = UUID.randomUUID();

    private static DataStore quickConnectStore;
    private static DataStoreEntry quickConnectEntry;

    public static void init() {
        quickConnectStore = AppCache.getNonNull("quickConnect", DataStore.class, () -> DataStoreProviders.byId("ssh")
                .orElseThrow()
                .defaultStore(
                        StoreViewState.get().getActiveCategory().getValue().getCategory()));
        quickConnectEntry = DataStoreEntry.createNew(
                STORE_ID, DataStorage.DEFAULT_CATEGORY_UUID, "quick-connect", quickConnectStore);
        DataStorage.get().addStoreEntryInProgress(quickConnectEntry);
    }

    public static void update(DataStore store) {
        quickConnectStore = store;
        DataStorage.get().updateEntryStore(quickConnectEntry, store);
        AppCache.update("quickConnect", store);
    }

    public static boolean launchQuickConnect(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }

        var provider = QuickConnectProvider.find(s);
        if (provider.isEmpty()) {
            return false;
        }

        var arguments = s.replaceFirst(provider.get().getName() + "\\s+", "").strip();
        if (arguments.isEmpty()) {
            return false;
        }

        var newStore = provider.get().createStore(arguments, quickConnectStore);
        if (newStore == null) {
            return false;
        }

        var existing = provider.get().findExisting(newStore);
        if (existing.isPresent()) {
            ThreadHelper.runAsync(() -> {
                try {
                    provider.get().open(existing.get());
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).handle();
                }
            });
            return true;
        }

        DataStorage.get().updateEntryStore(quickConnectEntry, newStore);
        if (provider.get().skipDialogIfPossible() && newStore.isComplete()) {
            update(newStore);
            ThreadHelper.runAsync(() -> {
                try {
                    DataStorage.get().addStoreEntryInProgress(quickConnectEntry);
                    provider.get().open(quickConnectEntry);
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).handle();
                } finally {
                    DataStorage.get().removeStoreEntryInProgress(quickConnectEntry);
                }
            });
            return true;
        }

        StoreCreationDialog.showEdit(quickConnectEntry, newStore, false, finished -> {
            update(finished.getStore());
            ThreadHelper.runAsync(() -> {
                try {
                    DataStorage.get().addStoreEntryInProgress(quickConnectEntry);
                    provider.get().open(quickConnectEntry);
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).handle();
                } finally {
                    DataStorage.get().removeStoreEntryInProgress(quickConnectEntry);
                }
            });
        });

        return true;
    }
}
