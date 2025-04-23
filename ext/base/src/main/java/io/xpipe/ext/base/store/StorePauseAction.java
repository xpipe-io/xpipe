package io.xpipe.ext.base.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.Value;

public class StorePauseAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<PauseableStore>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<PauseableStore> store) {
                return new Action(store);
            }

            @Override
            public Class<PauseableStore> getApplicableClass() {
                return PauseableStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<PauseableStore> store) {
                return AppI18n.observable("pause");
            }

            @Override
            public LabelGraphic getIcon(DataStoreEntryRef<PauseableStore> store) {
                return new LabelGraphic.IconGraphic("mdi2p-pause");
            }

            @Override
            public boolean requiresValidStore() {
                return false;
            }
        };
    }

    @Override
    public BatchDataStoreCallSite<?> getBatchDataStoreCallSite() {
        return new BatchDataStoreCallSite<PauseableStore>() {

            @Override
            public ObservableValue<String> getName() {
                return AppI18n.observable("pause");
            }

            @Override
            public LabelGraphic getIcon() {
                return new LabelGraphic.IconGraphic("mdi2p-pause");
            }

            @Override
            public Class<?> getApplicableClass() {
                return PauseableStore.class;
            }

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<PauseableStore> store) {
                return new Action(store);
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntryRef<PauseableStore> entry;

        @Override
        public void execute() throws Exception {
            entry.getStore().pause();
        }
    }
}
