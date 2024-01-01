package io.xpipe.app.browser;

import io.xpipe.app.core.AppCache;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
@Getter
public class BrowserSavedStateImpl implements BrowserSavedState {

    static BrowserSavedStateImpl load() {
        return AppCache.get("browser-state", BrowserSavedStateImpl.class, () -> {
            return new BrowserSavedStateImpl(FXCollections.observableArrayList());
        });
    }

    ObservableList<Entry> lastSystems;

    @Override
    public synchronized void add(BrowserSavedState.Entry entry) {
        lastSystems.removeIf(s -> s.getUuid().equals(entry.getUuid()));
        lastSystems.addFirst(entry);
        if (lastSystems.size() > 10) {
            lastSystems.removeLast();
        }
    }

    @Override
    public void save() {
        AppCache.update("browser-state", this);
    }

    @Override
    public ObservableList<Entry> getEntries() {
        return lastSystems;
    }
}
