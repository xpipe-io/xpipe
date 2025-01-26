package io.xpipe.ext.base.service;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.comp.store.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.*;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.ShellStoreFormat;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.store.DataStore;
import io.xpipe.ext.base.script.ScriptStore;
import io.xpipe.ext.base.script.SimpleScriptStore;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

import java.util.ArrayList;
import java.util.List;

public class ServiceControlStoreProvider implements SingletonSessionStoreProvider, DataStoreProvider {

    public String displayName(DataStoreEntry entry) {
        var s = (ServiceControlStore) entry.getStore();
        String n = entry.getName();
        return n + " (" + DataStorage.get().getStoreEntryDisplayName(s.getHost().get()) + ")";
    }

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.TUNNEL;
    }

    @Override
    public DataStoreEntry getSyntheticParent(DataStoreEntry store) {
        ServiceControlStore s = store.getStore().asNeeded();
        return DataStorage.get()
                .getOrCreateNewSyntheticEntry(
                        s.getHost().get(),
                        "Services",
                        CustomServiceGroupStore.builder().parent(s.getHost()).build());
    }

    @Override
    public String summaryString(StoreEntryWrapper wrapper) {
        ServiceControlStore s = wrapper.getEntry().getStore().asNeeded();
        return DataStoreFormatter.toApostropheName(s.getHost().get()) + " service control";
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        ServiceControlStore s = section.getWrapper().getEntry().getStore().asNeeded();
        return Bindings.createStringBinding(
                () -> {
                    var state = s.isSessionRunning()
                                    ? AppI18n.get("active")
                                    : s.isSessionEnabled() ? AppI18n.get("starting") : AppI18n.get("inactive");
                    return new ShellStoreFormat(null, state).format();
                },
                section.getWrapper().getCache(),
                AppPrefs.get().language());
    }


    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        ServiceControlStore st = store.getValue().asNeeded();
        var host = new SimpleObjectProperty<>(st.getHost());
        var start = new SimpleObjectProperty<>(st.getStartScript());
        var stop = new SimpleObjectProperty<>(st.getStopScript());
        var status = new SimpleObjectProperty<>(st.getStatusScript());
        var elevated = new SimpleBooleanProperty(st.isElevated());
        return new OptionsBuilder()
                .nameAndDescription("serviceHost")
                .addComp(
                        new StoreChoiceComp<>(
                                StoreChoiceComp.Mode.OTHER,
                                entry,
                                host,
                                ShellStore.class,
                                null,
                                StoreViewState.get().getAllConnectionsCategory()),
                        host)
                .nonNull()
                .nameAndDescription("serviceStartScript")
                .addComp(IntegratedTextAreaComp.script(host,start), start)
                .nonNull()
                .nameAndDescription("serviceStopScript")
                .addComp(IntegratedTextAreaComp.script(host, stop), stop)
                .nonNull()
                .nameAndDescription("serviceStatusScript")
                .addComp(IntegratedTextAreaComp.script(host, status), status)
                .nonNull()
                .nameAndDescription("serviceElevated")
                .addToggle(elevated)
                .bind(
                        () -> {
                            return ServiceControlStore.builder()
                                    .host(host.get())
                                    .startScript(start.get())
                                    .stopScript(stop.get())
                                    .statusScript(status.get())
                                    .elevated(elevated.get())
                                    .build();
                        },
                        store)
                .buildDialog();
    }

    @Override
    public String getDisplayIconFileName(DataStore store) {
        return "base:service_icon.svg";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(ServiceControlStore.class);
    }

    @Override
    public DataStore defaultStore() {
        return ServiceControlStore.builder().build();
    }

    @Override
    public String getId() {
        return "serviceControl";
    }

    @Override
    public DataStoreCreationCategory getCreationCategory() {
        return DataStoreCreationCategory.SERVICE;
    }

}
