package io.xpipe.ext.base.action;

import io.xpipe.app.browser.BrowserModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.ShellStore;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import lombok.Value;

public class BrowseStoreAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry entry;

        @Override
        public boolean requiresJavaFXPlatform() {
            return true;
        }

        @Override
        public void execute() {
            BrowserModel.DEFAULT.openFileSystemAsync(entry.getName(), entry.getStore().asNeeded(),null, new SimpleBooleanProperty());
            AppLayoutModel.get().selectBrowser();
        }
    }

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<ShellStore>() {

            @Override
            public boolean isMajor(ShellStore o) {
                return true;
            }

            @Override
            public ObservableValue<String> getName(ShellStore store) {
                return AppI18n.observable("browseFiles");
            }

            @Override
            public String getIcon(ShellStore store) {
                return "mdi2f-folder-open-outline";
            }

            @Override
            public ActionProvider.Action createAction(ShellStore store) {
                return new Action(DataStorage.get().getStoreEntry(store));
            }

            @Override
            public Class<ShellStore> getApplicableClass() {
                return ShellStore.class;
            }
        };
    }
}
