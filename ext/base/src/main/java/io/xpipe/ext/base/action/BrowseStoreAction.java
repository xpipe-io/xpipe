package io.xpipe.ext.base.action;

import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.FileSystemStore;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;

import lombok.Value;

public class BrowseStoreAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<ShellStore>() {

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
        public void execute() throws Exception {
            DataStoreEntryRef<FileSystemStore> replacement =
                    ProcessControlProvider.get().replace(entry.ref());
            BrowserSessionModel.DEFAULT.openFileSystemSync(replacement, null, new SimpleBooleanProperty());
            AppLayoutModel.get().selectBrowser();
        }
    }
}
