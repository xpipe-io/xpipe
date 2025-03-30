package io.xpipe.ext.base.action;

import io.xpipe.app.comp.store.StoreCreationDialog;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.ext.base.script.SimpleScriptStore;

import javafx.beans.value.ObservableValue;

import lombok.Value;

public class EditScriptStoreAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<SimpleScriptStore>() {

            @Override
            public boolean isSystemAction() {
                return true;
            }

            @Override
            public boolean isMajor(DataStoreEntryRef<SimpleScriptStore> o) {
                return true;
            }

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<SimpleScriptStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<SimpleScriptStore> getApplicableClass() {
                return SimpleScriptStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<SimpleScriptStore> store) {
                return AppI18n.observable("base.edit");
            }

            @Override
            public String getIcon(DataStoreEntryRef<SimpleScriptStore> store) {
                return "mdal-edit";
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
        public void execute() {
            StoreCreationDialog.showEdit(store);
        }
    }
}
