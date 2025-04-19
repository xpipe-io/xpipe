package io.xpipe.ext.base.service;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.store.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreUsageCategory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.DataStore;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

public abstract class AbstractServiceGroupStoreProvider implements DataStoreProvider {

    @Override
    public DocumentationLink getHelpLink() {
        return DocumentationLink.SERVICES;
    }

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.GROUP;
    }

    @Override
    public StoreEntryComp customEntryComp(StoreSection sec, boolean preferLarge) {
        var t = createToggleComp(sec);
        return StoreEntryComp.create(sec, t, preferLarge);
    }

    private StoreToggleComp createToggleComp(StoreSection sec) {
        var t = StoreToggleComp.<AbstractServiceGroupStore<?>>enableToggle(
                null, sec, new SimpleBooleanProperty(false), (g, aBoolean) -> {
                    var children =
                            DataStorage.get().getStoreChildren(sec.getWrapper().getEntry());
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
        t.setCustomVisibility(Bindings.createBooleanBinding(
                () -> {
                    var children =
                            DataStorage.get().getStoreChildren(sec.getWrapper().getEntry());
                    for (DataStoreEntry child : children) {
                        if (child.getStore() instanceof AbstractServiceStore serviceStore) {
                            if (serviceStore.getHost().getStore().requiresTunnel()) {
                                return true;
                            }
                        }
                    }
                    return false;
                },
                StoreViewState.get().getAllEntries().getList()));
        return t;
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        return Bindings.createStringBinding(
                () -> {
                    var all = section.getAllChildren().getList();
                    var shown = section.getShownChildren().getList();
                    if (shown.size() == 0) {
                        return null;
                    }

                    var string = all.size() == shown.size() ? all.size() : shown.size() + "/" + all.size();
                    return all.size() > 0
                            ? (all.size() == 1 ? AppI18n.get("hasService", string) : AppI18n.get("hasServices", string))
                            : AppI18n.get("noServices");
                },
                section.getShownChildren().getList(),
                section.getAllChildren().getList(),
                AppI18n.activeLanguage());
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
