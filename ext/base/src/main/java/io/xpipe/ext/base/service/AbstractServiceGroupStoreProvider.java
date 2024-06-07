package io.xpipe.ext.base.service;

import io.xpipe.app.comp.base.StoreToggleComp;
import io.xpipe.app.comp.base.SystemStateComp;
import io.xpipe.app.comp.store.StoreEntryComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.DataStore;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public abstract class AbstractServiceGroupStoreProvider implements DataStoreProvider {

    @Override
    public StoreEntryComp customEntryComp(StoreSection sec, boolean preferLarge) {
        var t = createToggleComp(sec);
        return StoreEntryComp.create(sec.getWrapper(), t, preferLarge);
    }

    private StoreToggleComp createToggleComp(StoreSection sec) {
        var enabled = new SimpleBooleanProperty();
        var t = new StoreToggleComp(null, null, sec, enabled, aBoolean -> {
            var children = DataStorage.get().getStoreChildren(sec.getWrapper().getEntry());
            ThreadHelper.runFailableAsync(() -> {
                for (DataStoreEntry child : children) {
                    if (child.getStore() instanceof AbstractServiceStore serviceStore) {
                        if (aBoolean) {
                            serviceStore.startSessionIfNeeded();
                        } else {
                            serviceStore.stopSessionIfNeeded();
                        }
                    }
                }
            });
        });
        t.setCustomVisibility(Bindings.createBooleanBinding(() -> {
            var children = DataStorage.get().getStoreChildren(sec.getWrapper().getEntry());
            for (DataStoreEntry child : children) {
                if (child.getStore() instanceof AbstractServiceStore serviceStore) {
                    if (serviceStore.getHost().getStore().requiresTunnel()) {
                        return true;
                    }
                }
            }
            return false;
        }, StoreViewState.get().getAllEntries().getList()));
        return t;
    }

    @Override
    public Comp<?> stateDisplay(StoreEntryWrapper w) {
        return new SystemStateComp(new SimpleObjectProperty<>(SystemStateComp.State.SUCCESS));
    }

    @Override
    public String getDisplayIconFileName(DataStore store) {
        return "base:serviceGroup_icon.svg";
    }

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        AbstractServiceGroupStore<?> s = store.getStore().asNeeded();
        return s.getParent().get();
    }
}
