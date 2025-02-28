package io.xpipe.ext.system.lxd;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.file.BrowserFileOpener;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.FilePath;

import javafx.beans.value.ObservableValue;

import lombok.Value;

public class LxdContainerEditRunConfigAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<LxdContainerStore>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<LxdContainerStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<LxdContainerStore> getApplicableClass() {
                return LxdContainerStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<LxdContainerStore> store) {
                return AppI18n.observable("editRunConfiguration");
            }

            @Override
            public String getIcon(DataStoreEntryRef<LxdContainerStore> store) {
                return "mdi2m-movie-edit";
            }

            @Override
            public boolean requiresValidStore() {
                return false;
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry store;

        @Override
        public void execute() throws Exception {
            var d = (LxdContainerStore) store.getStore();
            var elevatedRef = ProcessControlProvider.get()
                    .elevated(d.getCmd().getStore().getHost().get().ref());
            var file = new FilePath("/run/lxd/" + d.getContainerName() + "/lxc.conf");
            var model = BrowserFullSessionModel.DEFAULT.openFileSystemSync(
                    elevatedRef, m -> file.getParent(), null, true);
            var found = model.findFile(file.toString());
            if (found.isEmpty()) {
                return;
            }
            AppLayoutModel.get().selectBrowser();
            BrowserFileOpener.openInTextEditor(model, found.get());
        }
    }
}
