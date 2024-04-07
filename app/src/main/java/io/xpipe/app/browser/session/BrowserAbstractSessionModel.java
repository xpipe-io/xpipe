package io.xpipe.app.browser.session;

import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

@Getter
public class BrowserAbstractSessionModel<T extends BrowserSessionEntry<?>> {

    protected final ObservableList<T> sessionEntries = FXCollections.observableArrayList();
    protected final Property<T> selectedEntry = new SimpleObjectProperty<>();

    public void closeAsync(BrowserSessionEntry<?> e) {
        ThreadHelper.runAsync(() -> {
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

    void closeSync(BrowserSessionEntry<?> e) {
        e.close();
        synchronized (BrowserAbstractSessionModel.this) {
            this.sessionEntries.remove(e);
        }
    }
}
