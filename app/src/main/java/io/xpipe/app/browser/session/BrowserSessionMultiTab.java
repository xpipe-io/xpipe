package io.xpipe.app.browser.session;

import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import lombok.Getter;

@Getter
public class BrowserSessionMultiTab extends BrowserSessionTab<DataStore> {

    protected final Property<BrowserSessionTab<?>> currentTab = new SimpleObjectProperty<>();
    private final ObservableList<BrowserSessionTab<?>> allTabs = FXCollections.observableArrayList();

    public BrowserSessionMultiTab(BrowserAbstractSessionModel<?> browserModel, DataStoreEntryRef<?> entry) {
        super(browserModel, entry);
    }

    public Comp<?> comp() {
        var map = FXCollections.<Comp<?>, ObservableValue<Boolean>>observableHashMap();
        allTabs.addListener((ListChangeListener<? super BrowserSessionTab<?>>) c -> {
            for (BrowserSessionTab<?> a : c.getAddedSubList()) {
                map.put(a.comp(), BindingsHelper.map(currentTab, browserSessionTab -> a.equals(browserSessionTab)));
            }
        });
        var mt = new MultiContentComp(map);
        return mt;
    }

    public boolean canImmediatelyClose() {
        return true;
    }

    public void init() {}

    public void close() {}
}
