package io.xpipe.app.browser.session;

import io.xpipe.app.util.ThreadHelper;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

@Getter
public class BrowserAbstractSessionModel<T extends BrowserSessionEntry> {

    protected final ObservableList<T> sessionEntries = FXCollections.observableArrayList();
    protected final Property<T> selectedEntry = new SimpleObjectProperty<>();

    public void closeAsync(BrowserSessionEntry open) {
        ThreadHelper.runAsync(() -> {
            closeSync(open);
        });
    }

    void closeSync(BrowserSessionEntry open) {
        open.close();
        synchronized (BrowserAbstractSessionModel.this) {
            this.sessionEntries.remove(open);
        }
    }
}
