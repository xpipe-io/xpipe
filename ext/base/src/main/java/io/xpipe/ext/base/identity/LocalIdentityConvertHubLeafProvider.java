package io.xpipe.ext.base.identity;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class LocalIdentityConvertHubLeafProvider implements HubLeafProvider<LocalIdentityStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Override
    public boolean isMajor() {
        return true;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<LocalIdentityStore> o) {
        return DataStorage.get().supportsSync();
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<LocalIdentityStore> store) {
        return AppI18n.observable("sync");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<LocalIdentityStore> store) {
        return new LabelGraphic.IconGraphic("mdi2g-git");
    }

    @Override
    public Class<?> getApplicableClass() {
        return LocalIdentityStore.class;
    }

    @Override
    public AbstractAction createAction(DataStoreEntryRef<LocalIdentityStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public String getId() {
        return "convertLocalIdentity";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<LocalIdentityStore> {

        @Override
        public void executeImpl() {
            IdentityConvert.syncLocal(ref, true, ignored -> {});
        }
    }
}
