package io.xpipe.app.action;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.store.StoreCreationDialog;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.store.DataStore;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class EditStoreActionProvider implements LeafStoreActionProvider<DataStore> {

            @Override
            public AbstractAction createAction(DataStoreEntryRef<DataStore> ref) {
                return Action.builder().ref(ref).build();
            }

            @Override
            public Class<DataStore> getApplicableClass() {
                return DataStore.class;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<DataStore> store) {
                return AppI18n.observable("base.edit");
            }

            @Override
            public LabelGraphic getIcon(DataStoreEntryRef<DataStore> store) {
                return new LabelGraphic.IconGraphic("mdal-edit");
            }

            @Override
            public boolean requiresValidStore() {
                return false;
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<DataStore> o) {
                return true;
            }



        @Override
    public String getId() {
        return "editStore";
    }
@Jacksonized
@SuperBuilder
    static class Action extends StoreAction<DataStore> {

        @Override
        public void executeImpl() {
            StoreCreationDialog.showEdit(ref.get());
        }
    }
}
