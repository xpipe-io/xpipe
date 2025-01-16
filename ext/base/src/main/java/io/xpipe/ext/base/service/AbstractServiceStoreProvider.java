package io.xpipe.ext.base.service;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.store.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreUsageCategory;
import io.xpipe.app.ext.SingletonSessionStoreProvider;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.app.util.ShellStoreFormat;
import io.xpipe.core.store.DataStore;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;

import java.util.List;

public abstract class AbstractServiceStoreProvider implements SingletonSessionStoreProvider, DataStoreProvider {

    public String displayName(DataStoreEntry entry) {
        AbstractServiceStore s = entry.getStore().asNeeded();
        return DataStorage.get().getStoreEntryDisplayName(s.getHost().get()) + " - Port " + s.getRemotePort();
    }

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.TUNNEL;
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

                    if (!s.isSessionEnabled() || (s.isSessionEnabled() && !s.isSessionRunning())) {
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
        return Bindings.createStringBinding(
                () -> {
                    var desc = formatService(s);
                    var type = s.getServiceProtocolType() != null
                                    && !(s.getServiceProtocolType() instanceof ServiceProtocolType.None)
                            ? AppI18n.get(s.getServiceProtocolType().getTranslationKey())
                            : null;
                    var state = !s.requiresTunnel()
                            ? null
                            : s.isSessionRunning()
                                    ? AppI18n.get("active")
                                    : s.isSessionEnabled() ? AppI18n.get("starting") : AppI18n.get("inactive");
                    return new ShellStoreFormat(null, desc, type, state).format();
                },
                section.getWrapper().getCache(),
                AppI18n.activeLanguage());
    }

    protected String formatService(AbstractServiceStore s) {
        var desc = s.getLocalPort() != null
                ? "localhost:" + s.getLocalPort() + " <- :" + s.getRemotePort()
                : s.isSessionRunning()
                        ? "localhost:" + s.getSession().getLocalPort() + " <- :" + s.getRemotePort()
                        : AppI18n.get("servicePort", s.getRemotePort());
        return desc;
    }

    @Override
    public String getDisplayIconFileName(DataStore store) {
        return "base:service_icon.svg";
    }
}
