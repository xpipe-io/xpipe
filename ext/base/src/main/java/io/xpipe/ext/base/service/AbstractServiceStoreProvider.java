package io.xpipe.ext.base.service;

import io.xpipe.app.comp.base.SystemStateComp;
import io.xpipe.app.comp.store.DenseStoreEntryComp;
import io.xpipe.app.comp.store.StoreEntryComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreUsageCategory;
import io.xpipe.app.ext.SingletonSessionStoreProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.core.store.DataStore;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public abstract class AbstractServiceStoreProvider implements SingletonSessionStoreProvider, DataStoreProvider {

    public String browserDisplayName(DataStoreEntry entry) {
        AbstractServiceStore s = entry.getStore().asNeeded();
        return DataStorage.get().getStoreEntryDisplayName(s.getHost().get()) + " - Port " + s.getRemotePort();
    }

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.TUNNEL;
    }

    @Override
    public ActionProvider.Action launchAction(DataStoreEntry store) {
        return new ActionProvider.Action() {
            @Override
            public void execute() throws Exception {
                AbstractServiceStore s = store.getStore().asNeeded();
                s.startSessionIfNeeded();
            }
        };
    }

    @Override
    public DataStoreEntry getSyntheticParent(DataStoreEntry store) {
        AbstractServiceStore s = store.getStore().asNeeded();
        return DataStorage.get()
                .getOrCreateNewSyntheticEntry(
                        s.getHost().get(),
                        "Services",
                        CustomServiceGroupStore.builder().parent(s.getHost()).build());
    }

    @Override
    public Comp<?> stateDisplay(StoreEntryWrapper w) {
        return new SystemStateComp(Bindings.createObjectBinding(
                () -> {
                    AbstractServiceStore s = w.getEntry().getStore().asNeeded();
                    if (!s.requiresTunnel()) {
                        return SystemStateComp.State.SUCCESS;
                    }

                    if (!s.isSessionEnabled()) {
                        return SystemStateComp.State.OTHER;
                    }

                    return s.isSessionRunning() ? SystemStateComp.State.SUCCESS : SystemStateComp.State.FAILURE;
                },
                w.getCache()));
    }

    @Override
    public StoreEntryComp customEntryComp(StoreSection sec, boolean preferLarge) {
        var toggle = createToggleComp(sec);
        toggle.setCustomVisibility(Bindings.createBooleanBinding(
                () -> {
                    AbstractServiceStore s =
                            sec.getWrapper().getEntry().getStore().asNeeded();
                    if (!s.getHost().getStore().requiresTunnel()) {
                        return false;
                    }

                    return true;
                },
                sec.getWrapper().getCache()));
        return new DenseStoreEntryComp(sec, true, toggle);
    }

    @Override
    public List<String> getSearchableTerms(DataStore store) {
        AbstractServiceStore s = store.asNeeded();
        return s.getLocalPort() != null
                ? List.of("" + s.getRemotePort(), "" + s.getLocalPort())
                : List.of("" + s.getRemotePort());
    }

    @Override
    public String summaryString(StoreEntryWrapper wrapper) {
        AbstractServiceStore s = wrapper.getEntry().getStore().asNeeded();
        return DataStoreFormatter.toApostropheName(s.getHost().get()) + " service";
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        AbstractServiceStore s = section.getWrapper().getEntry().getStore().asNeeded();
        if (s.getLocalPort() != null) {
            return new SimpleStringProperty("Port " + s.getLocalPort() + " <- " + s.getRemotePort());
        } else {
            return new SimpleStringProperty("Port " + s.getRemotePort());
        }
    }

    @Override
    public String getDisplayIconFileName(DataStore store) {
        return "base:service_icon.svg";
    }
}
