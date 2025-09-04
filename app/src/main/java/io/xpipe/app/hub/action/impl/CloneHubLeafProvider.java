package io.xpipe.app.hub.action.impl;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.hub.comp.StoreCreationDialog;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.platform.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;

public class CloneHubLeafProvider implements HubLeafProvider<DataStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CONFIGURATION;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<DataStore> o) {
        return o.get().getProvider().canClone() && o.get().getBreakOutCategory() == null;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<DataStore> store) {
        return AppI18n.observable("base.clone");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<DataStore> store) {
        return new LabelGraphic.IconGraphic("mdi2c-content-copy");
    }

    @Override
    public Class<DataStore> getApplicableClass() {
        return DataStore.class;
    }

    @Override
    public String getId() {
        return "cloneStore";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<DataStore> {

        @Override
        public void executeImpl() {
            var entry = DataStoreEntry.createNew(
                    ref.get().getName() + " (" + AppI18n.get("connectionCopy") + ")", ref.getStore());

            entry.setIcon(ref.get().getIcon(), true);
            entry.setColor(ref.get().getColor());
            entry.setExpanded(ref.get().isExpanded());
            entry.setFreeze(ref.get().isFreeze());
            entry.setCategoryUuid(ref.get().getCategoryUuid());
            entry.setPinToTop(ref.get().isPinToTop());
            entry.setOrderIndex(ref.get().getOrderIndex());
            entry.setNotes(ref.get().getNotes());

            var instant = ref.get().getLastAccess().plus(Duration.ofSeconds(1));
            entry.setLastModified(instant);
            entry.setLastUsed(instant);

            DataStorage.get().addStoreEntryIfNotPresent(entry);
            StoreCreationDialog.showEdit(entry);
        }
    }
}
