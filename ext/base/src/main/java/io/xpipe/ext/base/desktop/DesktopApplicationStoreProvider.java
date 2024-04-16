package io.xpipe.ext.base.desktop;

import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.fxcomps.impl.DataStoreChoiceComp;
import io.xpipe.app.storage.ContextualFileReference;
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
    public ActionProvider.Action browserAction(BrowserSessionModel sessionModel, DataStoreEntry store, BooleanProperty busy) {
        DesktopApplicationStore s = store.getStore().asNeeded();
        return new ActionProvider.Action() {
            @Override
            public boolean requiresJavaFXPlatform() {
                return false;
            }

            @Override
            public void execute() throws Exception {
                s.getDesktop().getStore().runDesktopScript(store.getName(), s.getDesktop().getStore().getUsedDialect(), s.getFullCommand());
            }
        };
    }

    @Override
    public ActionProvider.Action launchAction(DataStoreEntry store) {
        DesktopApplicationStore s = store.getStore().asNeeded();
        return new ActionProvider.Action() {
            @Override
            public boolean requiresJavaFXPlatform() {
                return false;
            }

            @Override
            public void execute() throws Exception {
                s.getDesktop().getStore().runDesktopScript(store.getName(), s.getDesktop().getStore().getUsedDialect(), s.getFullCommand());
            }
        };
    }

    @Override
    public CreationCategory getCreationCategory() {
        return CreationCategory.DESKSTOP;
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
        var path = new SimpleStringProperty(st.getPath() != null ? st.getPath().toAbsoluteFilePath(null) : null);
        var args = new SimpleStringProperty(st.getArguments() != null ? st.getArguments() : null);
        return new OptionsBuilder()
                .nameAndDescription("desktopEnvironmentBase")
                .addComp(
                        new DataStoreChoiceComp<>(
                                DataStoreChoiceComp.Mode.HOST,
                                entry,
                                host,
                                DesktopBaseStore.class,
                                desktopStoreDataStoreEntryRef -> desktopStoreDataStoreEntryRef.getStore().supportsDesktopAccess(),
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
                            return DesktopApplicationStore.builder().desktop(host.get()).path(ContextualFileReference.of(path.get())).arguments(args.get()).build();
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
    public DataStore defaultStore() {
        return DesktopApplicationStore.builder().build();
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("desktopApplication");
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(DesktopApplicationStore.class);
    }


}
