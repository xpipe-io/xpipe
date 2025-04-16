package io.xpipe.ext.base.desktop;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.comp.store.StoreChoiceComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.ext.*;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.store.DataStore;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.List;

public class DesktopApplicationStoreProvider implements DataStoreProvider {

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.DESKTOP;
    }

    @Override
    public ActionProvider.Action browserAction(
            BrowserFullSessionModel sessionModel, DataStoreEntry store, BooleanProperty busy) {
        return launchAction(store);
    }

    @Override
    public ActionProvider.Action launchAction(DataStoreEntry store) {
        return new ActionProvider.Action() {

            @Override
            public void execute() throws Exception {
                DesktopApplicationStore s = store.getStore().asNeeded();
                var baseEntry = s.getDesktop().get();
                var baseActivate = baseEntry.getProvider().activateAction(baseEntry);
                if (baseActivate != null) {
                    baseActivate.execute();
                }
                s.getDesktop().getStore().runDesktopApplication(store.getName(), s);
            }
        };
    }

    @Override
    public DataStoreCreationCategory getCreationCategory() {
        return DataStoreCreationCategory.DESKTOP;
    }

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        DesktopApplicationStore s = store.getStore().asNeeded();
        return s.getDesktop().get();
    }

    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        DesktopApplicationStore st = (DesktopApplicationStore) store.getValue();
        var host = new SimpleObjectProperty<>(st.getDesktop());
        var path = new SimpleStringProperty(st.getPath());
        var args = new SimpleStringProperty(st.getArguments() != null ? st.getArguments() : null);
        return new OptionsBuilder()
                .nameAndDescription("desktopBase")
                .addComp(
                        new StoreChoiceComp<>(
                                StoreChoiceComp.Mode.HOST,
                                entry,
                                host,
                                DesktopBaseStore.class,
                                desktopStoreDataStoreEntryRef ->
                                        desktopStoreDataStoreEntryRef.getStore().supportsDesktopAccess(),
                                StoreViewState.get().getAllConnectionsCategory()),
                        host)
                .nonNull()
                .nameAndDescription("desktopApplicationPath")
                .addString(path)
                .nonNull()
                .nameAndDescription("desktopApplicationArguments")
                .addString(args)
                .bind(
                        () -> {
                            return DesktopApplicationStore.builder()
                                    .desktop(host.get())
                                    .path(path.get())
                                    .arguments(args.get())
                                    .build();
                        },
                        store)
                .buildDialog();
    }

    public String summaryString(StoreEntryWrapper wrapper) {
        DesktopApplicationStore s = wrapper.getEntry().getStore().asNeeded();
        return DataStoreFormatter.toApostropheName(s.getDesktop().get()) + " config";
    }

    @Override
    public String getDisplayIconFileName(DataStore store) {
        return "base:desktopApplication_icon.svg";
    }

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        return DesktopApplicationStore.builder().build();
    }

    @Override
    public String getId() {
        return "desktopApplication";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(DesktopApplicationStore.class);
    }
}
