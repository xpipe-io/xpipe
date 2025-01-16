package io.xpipe.ext.base.action;

import io.xpipe.app.comp.store.StoreIconChoiceDialog;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;

import lombok.Value;

public class ChangeStoreIconAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<>() {

            @Override
            public boolean isSystemAction() {
                return true;
            }

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<DataStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<DataStore> getApplicableClass() {
                return DataStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<DataStore> store) {
                return AppI18n.observable("base.changeIcon");
            }

            @Override
            public String getIcon(DataStoreEntryRef<DataStore> store) {
                return "mdi2t-tooltip-image-outline";
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry entry;

        @Override
        public void execute() {
            Platform.runLater(() -> {
                StoreIconChoiceDialog.show(entry);
            });
        }
    }
}
