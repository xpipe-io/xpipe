package io.xpipe.app.browser;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public interface BrowserSavedState {

    static BrowserSavedState none() {
        return new BrowserSavedState() {
            @Override
            public void add(Entry entry) {

            }

            @Override
            public void save() {

            }

            @Override
            public ObservableList<Entry> getEntries() {
                return FXCollections.observableArrayList();
            }
        };
    }

    public void add(Entry entry);

    void save();

    ObservableList<Entry> getEntries();

    @Value
    @Jacksonized
    @Builder
    public static class Entry {

        UUID uuid;
        String path;
    }
}
