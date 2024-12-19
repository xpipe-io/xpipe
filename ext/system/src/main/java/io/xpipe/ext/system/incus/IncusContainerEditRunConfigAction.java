package io.xpipe.ext.system.incus;

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

public class IncusContainerEditRunConfigAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<IncusContainerStore>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<IncusContainerStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<IncusContainerStore> getApplicableClass() {
                return IncusContainerStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<IncusContainerStore> store) {
                return AppI18n.observable("editRunConfiguration");
            }

            @Override
            public String getIcon(DataStoreEntryRef<IncusContainerStore> store) {
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
            var d = (IncusContainerStore) store.getStore();
            var elevatedRef = ProcessControlProvider.get().elevated(d.getInstall().getStore().getHost().get().ref());
            var file = new FilePath("/run/incus/" + d.getContainerName() + "/lxc.conf");
            var model = BrowserFullSessionModel.DEFAULT.openFileSystemSync(elevatedRef, m -> file.getParent().toString(), null, true);
            var found = model.findFile(file.toString());
            if (found.isEmpty()) {
                return;
            }
            AppLayoutModel.get().selectBrowser();
            BrowserFileOpener.openInTextEditor(model, found.get());
        }
    }
}
