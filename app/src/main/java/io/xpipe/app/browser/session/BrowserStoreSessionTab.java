package io.xpipe.app.browser.session;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.storage.DataColor;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;

@Getter
public abstract class BrowserStoreSessionTab<T extends DataStore> extends BrowserSessionTab {

    protected final DataStoreEntryRef<? extends T> entry;

    public BrowserStoreSessionTab(BrowserAbstractSessionModel<?> browserModel, DataStoreEntryRef<? extends T> entry) {
        super(browserModel, DataStorage.get().getStoreEntryDisplayName(entry.get()), DataStorage.get().getStorePath(entry.getEntry()).toString());
        this.entry = entry;
    }

    public abstract Comp<?> comp();

    public abstract boolean canImmediatelyClose();

    public abstract void init() throws Exception;

    public abstract void close();

    @Override
    public String getIcon() {
        return entry.get().getEffectiveIconFile();
    }

    @Override
    public DataColor getColor() {
        return DataStorage.get().getEffectiveColor(entry.get());
    }
}
