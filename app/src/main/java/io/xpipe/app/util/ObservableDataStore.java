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
            var timer = getCache("oberserverTimer", Timer.class, null);
            if (timer == null) {
                timer = new Timer(true);
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (getObserverState()) {
                            refresh();
                        }
                    }
                }, 0, 20000);
                setCache("oberserverTimer", timer);
            }
        }
    }

    default boolean getObserverState() {
        return getCache("observerState", Boolean.class, false);
    }

    default void setObserverState(boolean state) {
        setCache("observerState", state);
    }

    private void refresh() {
        var entry = DataStorage.get().getStoreEntry(this);
        DataStorage.get().refreshChildren(entry);
    }
}
