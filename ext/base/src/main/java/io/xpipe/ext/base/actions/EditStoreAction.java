package io.xpipe.ext.base.actions;

import io.xpipe.app.comp.source.store.GuiDsStoreCreator;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.ActionProvider;
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
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<DataStore>() {

            @Override
            public boolean isMajor() {
                return true;
            }

            @Override
            public boolean showIfDisabled() {
                return false;
            }

            @Override
            public ActionProvider.Action createAction(DataStore store) {
                return new Action(DataStorage.get().getStore(store));
            }

            @Override
            public Class<DataStore> getApplicableClass() {
                return DataStore.class;
            }

            @Override
            public boolean isApplicable(DataStore o) throws Exception {
                return DataStorage.get().getStore(o).getConfiguration().isEditable();
            }

            @Override
            public ObservableValue<String> getName(DataStore store) {
                return I18n.observable("base.edit");
            }

            @Override
            public String getIcon(DataStore store) {
                return "mdal-edit";
            }
        };
    }
}
