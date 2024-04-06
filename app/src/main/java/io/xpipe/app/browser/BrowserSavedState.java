package io.xpipe.app.browser;

import javafx.collections.ObservableList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public interface BrowserSavedState {

    void add(Entry entry);

    void save();

    ObservableList<Entry> getEntries();

    @Value
    @Jacksonized
    @Builder
    @AllArgsConstructor
    class Entry {

        UUID uuid;
        String path;
    }
}
