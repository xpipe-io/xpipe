package io.xpipe.ext.base.store;

import atlantafx.base.theme.Styles;
import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.hub.comp.StoreCreationDialog;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class SyncConfigHubLeafProvider implements HubLeafProvider<SyncConfigStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Override
    public boolean isMajor() {
        return true;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<SyncConfigStore> o) {
        return DataStorage.get().supportsSync() && !o.get().getProvider().isSyncable(o.get());
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<SyncConfigStore> store) {
        return AppI18n.observable("sync");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<SyncConfigStore> store) {
        return new LabelGraphic.IconGraphic("mdi2g-git");
    }

    @Override
    public Class<?> getApplicableClass() {
        return SyncConfigStore.class;
    }

    @Override
    public AbstractAction createAction(DataStoreEntryRef<SyncConfigStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public String getId() {
        return "syncConfig";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<SyncConfigStore> {

        @Override
        public void executeImpl() {
            StoreCreationDialog.showEdit(ref.get());

            // Ugly solution to sync key file if needed
            Platform.runLater(() -> {
                var found = AppMainWindow.get().getStage().getScene().getRoot().lookupAll(".git-sync-file-button");
                if (found.size() != 1) {
                    return;
                }

                var first = found.iterator().next();
                if (first instanceof Button b) {
                    b.requestFocus();
                    b.getStyleClass().add(Styles.ACCENT);
                }
            });
        }
    }
}
