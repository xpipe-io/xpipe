package io.xpipe.ext.base.actions;

import io.xpipe.app.comp.source.store.GuiDsStoreCreator;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;
import javafx.beans.value.ObservableValue;
import lombok.Value;

public class EditStoreAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry store;

        @Override
        public boolean requiresPlatform() {
            return true;
        }

        @Override
        public void execute() throws Exception {
            GuiDsStoreCreator.showEdit(store);
        }
    }


    @Override
    public DefaultDataStoreCallSite<?> getDefaultDataStoreCallSite() {
        return new DefaultDataStoreCallSite<DataStore>() {
            @Override
            public boolean isApplicable(DataStore o) {
                return DataStorage.get().getStoreEntryIfPresent(o).orElseThrow().getState().equals(DataStoreEntry.State.INCOMPLETE);
            }

            @Override
            public ActionProvider.Action createAction(DataStore store) {
                return new Action(DataStorage.get().getStoreEntryIfPresent(store).orElseThrow());
            }

            @Override
            public Class<DataStore> getApplicableClass() {
                return DataStore.class;
            }
        };
    }

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<DataStore>() {

            @Override
            public boolean isMajor() {
                return false;
            }

            @Override
            public ActiveType activeType() {
                return ActiveType.ALWAYS_ENABLE;
            }

            @Override
            public ActionProvider.Action createAction(DataStore store) {
                return new Action(DataStorage.get().getStoreEntry(store));
            }

            @Override
            public Class<DataStore> getApplicableClass() {
                return DataStore.class;
            }

            @Override
            public boolean isApplicable(DataStore o) throws Exception {
                return DataStorage.get().getStoreEntry(o).getConfiguration().isEditable();
            }

            @Override
            public ObservableValue<String> getName(DataStore store) {
                return AppI18n.observable("base.edit");
            }

            @Override
            public String getIcon(DataStore store) {
                return "mdal-edit";
            }
        };
    }
}
