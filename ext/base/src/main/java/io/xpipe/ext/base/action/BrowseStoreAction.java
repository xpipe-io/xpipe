package io.xpipe.ext.base.action;

import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.process.ShellStoreState;
import io.xpipe.core.store.ShellStore;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import lombok.Value;

public class BrowseStoreAction implements ActionProvider {

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<ShellStore>() {

            @Override
            public boolean isApplicable(DataStoreEntryRef<ShellStore> o) {
                var state = o.get().getStorePersistentState();
                if (state instanceof ShellStoreState shellStoreState) {
                    return shellStoreState.getShellDialect() == null ||
                            shellStoreState.getShellDialect().getDumbMode().supportsAnyPossibleInteraction();
                } else {
                    return true;
                }
            }

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<ShellStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<ShellStore> getApplicableClass() {
                return ShellStore.class;
            }

            @Override
            public boolean isMajor(DataStoreEntryRef<ShellStore> o) {
                return true;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
                return AppI18n.observable("browseFiles");
            }

            @Override
            public String getIcon(DataStoreEntryRef<ShellStore> store) {
                return "mdi2f-folder-open-outline";
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry entry;

        @Override
        public boolean requiresJavaFXPlatform() {
            return true;
        }

        @Override
        public void execute() {
            BrowserSessionModel.DEFAULT.openFileSystemAsync(entry.ref(), null, new SimpleBooleanProperty());
            AppLayoutModel.get().selectBrowser();
        }
    }
}
