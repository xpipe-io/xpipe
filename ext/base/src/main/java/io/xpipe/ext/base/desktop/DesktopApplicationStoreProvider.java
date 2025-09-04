package io.xpipe.ext.base.desktop;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.ext.*;
import io.xpipe.app.hub.comp.StoreChoiceComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.core.FailableRunnable;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.List;

public class DesktopApplicationStoreProvider implements DataStoreProvider {

    @Override
    public DocumentationLink getHelpLink() {
        return DocumentationLink.DESKTOP_APPLICATIONS;
    }

    @Override
    public FailableRunnable<Exception> launch(DataStoreEntry store) {
        return () -> {
            DesktopApplicationStore s = store.getStore().asNeeded();
            var baseEntry = s.getDesktop().get();
            var baseActivate = baseEntry.getProvider().activateAction(baseEntry);
            if (baseActivate != null) {
                baseActivate.run();
            }
            s.getDesktop().getStore().runDesktopApplication(store.getName(), s);
        };
    }

    @Override
    public FailableRunnable<Exception> launchBrowser(
            BrowserFullSessionModel sessionModel, DataStoreEntry store, BooleanProperty busy) {
        return launch(store);
    }

    @Override
    public DataStoreCreationCategory getCreationCategory() {
        return DataStoreCreationCategory.DESKTOP;
    }

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.DESKTOP;
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
