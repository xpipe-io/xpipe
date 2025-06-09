package io.xpipe.ext.system.lxd;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.hub.action.LeafStoreActionProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.ext.system.incus.IncusCommandView;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class LxdContainerEditConfigActionProvider implements LeafStoreActionProvider<LxdContainerStore> {

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
        return AppI18n.observable("editConfiguration");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<LxdContainerStore> store) {
        return new LabelGraphic.IconGraphic("mdi2f-file-document-edit");
    }

    @Override
    public boolean requiresValidStore() {
        return false;
    }

    @Override
    public String getId() {
        return "editLxdContainerConfig";
    }

    @Jacksonized
    @SuperBuilder
    static class Action extends StoreAction<LxdContainerStore> {

        @Override
        public void executeImpl() throws Exception {
            var d = (LxdContainerStore) ref.getStore();
            var view = new IncusCommandView(
                    d.getCmd().getStore().getHost().getStore().getOrStartSession());
            TerminalLauncher.open(ref.get().getName(), view.configEdit(d.getContainerName()));
        }
    }
}
