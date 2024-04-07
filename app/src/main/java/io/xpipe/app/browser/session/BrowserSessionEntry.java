package io.xpipe.app.browser.session;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;

@Getter
public abstract class BrowserSessionEntry<T extends DataStore> {

    protected final DataStoreEntryRef<? extends T> entry;
    protected final BooleanProperty busy = new SimpleBooleanProperty();
    protected final BrowserAbstractSessionModel<?> browserModel;
    protected final String name;
    protected final String tooltip;

    public BrowserSessionEntry(BrowserAbstractSessionModel<?> browserModel, DataStoreEntryRef<? extends T> entry) {
        this.browserModel = browserModel;
        this.entry = entry;
        this.name = DataStorage.get().getStoreDisplayName(entry.get());
        this.tooltip = DataStorage.get().getId(entry.getEntry()).toString();
    }

    public abstract Comp<?> comp();

    public abstract boolean canImmediatelyClose();

    public abstract void init() throws Exception;

    public abstract void close();
}
