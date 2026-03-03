package io.xpipe.app.hub.comp;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.ext.*;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.StorageListener;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StoreQuickConnect {

    public static final UUID STORE_ID = UUID.randomUUID();

    private static DataStore quickConnectStore;
    private static DataStoreEntry quickConnectEntry;

    public static void init() {
        quickConnectStore = AppCache.getNonNull("quickConnect", DataStore.class, () -> DataStoreProviders.byId("ssh").orElseThrow()
                .defaultStore(StoreViewState.get().getActiveCategory().getValue()
                .getCategory()));
        quickConnectEntry = DataStoreEntry.createNew(STORE_ID, DataStorage.DEFAULT_CATEGORY_UUID, "quick-connect", quickConnectStore);
        DataStorage.get().addStoreEntryInProgress(quickConnectEntry);
    }

    public static void update(DataStore store) {
        quickConnectStore = store;
        DataStorage.get().updateEntryStore(quickConnectEntry, store);
        AppCache.update("quickConnect", store);
    }

    public static boolean launchQuickConnect(String s) {
        if (s == null || s.isBlank() || !s.contains("@")) {
            return false;
        }

        if (s.startsWith("ssh ")) {
            s = s.substring(4);
        }

        var split = s.split("@", 2);
        if (split.length != 2) {
            return false;
        }

        var user = split[0];

        var target = split[1];
        String host = target;
        Integer port = null;

        if (StringUtils.countMatches(target, ":") == 1 || (target.contains("[") && target.contains("]"))) {
            var index = target.lastIndexOf(":");
            host = target.substring(0, index);
            try {
                port = Integer.parseInt(target.substring(index + 1));
            } catch (NumberFormatException ignored) {
            }
        }

        var newStore = ProcessControlProvider.get().quickConnectStore(user, host, port, quickConnectStore);
        DataStorage.get().updateEntryStore(quickConnectEntry, newStore);

        var model = StoreCreationDialog.showEdit(quickConnectEntry, newStore, false, finished -> {
            ThreadHelper.runAsync(() -> {
                try {
                    quickConnectEntry.getProvider().launch(quickConnectEntry).run();
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).handle();
                }
            });
        });
        GlobalTimer.delay(() -> {
            Platform.runLater(() -> {
                model.finish();
            });
        }, Duration.ofMillis(100));
        return true;
    }
}
