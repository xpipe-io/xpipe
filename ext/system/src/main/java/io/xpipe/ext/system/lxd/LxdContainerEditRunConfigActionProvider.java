package io.xpipe.ext.system.lxd;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.hub.action.LeafStoreActionProvider;
import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.file.BrowserFileOpener;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.store.FilePath;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class LxdContainerEditRunConfigActionProvider implements LeafStoreActionProvider<LxdContainerStore> {

    @Override
    public boolean isMutation() {
        return true;
    }

    @Override
    public AbstractAction createAction(DataStoreEntryRef<LxdContainerStore> ref) {
        return Action.builder().ref(ref).build();
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
    public LabelGraphic getIcon(DataStoreEntryRef<LxdContainerStore> store) {
        return new LabelGraphic.IconGraphic("mdi2m-movie-edit");
    }

    @Override
    public boolean requiresValidStore() {
        return false;
    }

    @Override
    public String getId() {
        return "editLxdContainerRunConfig";
    }

    @Jacksonized
    @SuperBuilder
    static class Action extends StoreAction<LxdContainerStore> {

        @Override
        public void executeImpl() throws Exception {
            var d = (LxdContainerStore) ref.getStore();
            var elevatedRef = ProcessControlProvider.get()
                    .elevated(d.getCmd().getStore().getHost().get().ref());
            var file = FilePath.of("/run/lxd/" + d.getContainerName() + "/lxc.conf");
            var model =
                    BrowserFullSessionModel.DEFAULT.openFileSystemSync(elevatedRef, m -> file.getParent(), null, true);
            var found = model.findFile(file);
            if (found.isEmpty()) {
                return;
            }
            AppLayoutModel.get().selectBrowser();
            BrowserFileOpener.openInTextEditor(model, found.get());
        }
    }
}
