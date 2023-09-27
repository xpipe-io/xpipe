package io.xpipe.app.util;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.InternalCacheDataStore;

import java.util.Timer;
import java.util.TimerTask;

public interface ObservableDataStore extends DataStore, InternalCacheDataStore {

    default void toggleObserverState(boolean state) {
        setObserverState(state);
        var entry = DataStorage.get().getStoreEntry(this);
        entry.setObserving(state);
        if (state) {
            var timer = getState("oberserverTimer", Timer.class, null);
            if (timer == null) {
                timer = new Timer(true);
                timer.scheduleAtFixedRate(
                        new TimerTask() {
                            @Override
                            public void run() {
                                if (getObserverState()) {
                                    refresh();
                                }
                            }
                        },
                        0,
                        20000);
                setState("oberserverTimer", timer);
            }
        }
    }

    default void setObserverState(boolean state) {
        setState("observerState", state);
    }

    default boolean getObserverState() {
        return getState("observerState", Boolean.class, false);
    }

    private void refresh() {
        var entry = DataStorage.get().getStoreEntry(this);
        if (DataStorage.get().refresh(entry, true)) {
            DataStorage.get().refreshChildren(entry);
        }
    }
}
