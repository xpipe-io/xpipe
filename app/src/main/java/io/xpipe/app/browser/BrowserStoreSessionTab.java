package io.xpipe.app.browser;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import lombok.Getter;

@Getter
public abstract class BrowserStoreSessionTab<T extends DataStore> extends BrowserSessionTab {

    protected final DataStoreEntryRef<? extends T> entry;
    private final String name;

    public BrowserStoreSessionTab(BrowserAbstractSessionModel<?> browserModel, DataStoreEntryRef<? extends T> entry) {
        super(browserModel);
        this.entry = entry;
        this.name = DataStorage.get().getStoreEntryDisplayName(entry.get());
    }

    @Override
    public ObservableValue<String> getName() {
        return new SimpleStringProperty(name);
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
    public DataStoreColor getColor() {
        return DataStorage.get().getEffectiveColor(entry.get());
    }
}
