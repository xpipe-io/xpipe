package io.xpipe.app.browser;

import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class BrowserAbstractSessionModel<T extends BrowserSessionTab> {

    protected final ObservableList<T> sessionEntries = FXCollections.observableArrayList();
    protected final Property<T> selectedEntry = new SimpleObjectProperty<>();
    protected final BooleanProperty busy = new SimpleBooleanProperty();

    public void closeAsync(BrowserSessionTab e) {
        ThreadHelper.runAsync(() -> {
            // This is a bit ugly
            // If we die on tab init, wait a bit with closing to avoid removal while it is still being inited/added
            ThreadHelper.sleep(100);
            closeSync(e);
        });
    }

    public void openSync(T e, BooleanProperty externalBusy) throws Exception {
        try (var b = new BooleanScope(externalBusy != null ? externalBusy : new SimpleBooleanProperty()).start()) {
            e.init();
            // Prevent multiple calls from interfering with each other
            synchronized (this) {
                sessionEntries.add(e);
                // The tab pane doesn't automatically select new tabs
                selectedEntry.setValue(e);
            }
        }
    }

    public void closeSync(BrowserSessionTab e) {
        e.close();
        synchronized (BrowserAbstractSessionModel.this) {
            this.sessionEntries.remove(e);
        }
    }

    public List<T> getSessionEntriesSnapshot() {
        synchronized (this) {
            return new ArrayList<>(sessionEntries);
        }
    }
}
