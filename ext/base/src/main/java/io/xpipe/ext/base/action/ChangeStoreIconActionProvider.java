package io.xpipe.ext.base.action;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.action.LeafStoreActionProvider;
import io.xpipe.app.action.StoreAction;
import io.xpipe.app.comp.store.StoreIconChoiceDialog;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.store.DataStore;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class ChangeStoreIconActionProvider implements LeafStoreActionProvider<DataStore> {

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
        return AppI18n.observable("base.changeIcon");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<DataStore> store) {
                return new LabelGraphic.IconGraphic("mdi2t-tooltip-image-outline");
            }

        @Override
    public String getId() {
        return "changeStoreIcon";
    }
@Jacksonized
@SuperBuilder
    static class Action extends StoreAction<DataStore> {

        @Override
        public void executeImpl() {
            Platform.runLater(() -> {
                StoreIconChoiceDialog.show(ref.get());
            });
        }
    }
}
