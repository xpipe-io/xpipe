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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiIdentityConvertHubLeafProvider implements HubLeafProvider<MultiIdentityStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Override
    public boolean isMajor() {
        return true;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<MultiIdentityStore> o) {
        return DataStorage.get().supportsSync()
                && o.getStore().areAnyChildrenLocal()
                && o.getStore().areAllIdentitiesAccessible()
                && !o.getStore().hasNestedMultiIdentities();
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<MultiIdentityStore> store) {
        return AppI18n.observable("sync");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<MultiIdentityStore> store) {
        return new LabelGraphic.IconGraphic("mdi2g-git");
    }

    @Override
    public Class<?> getApplicableClass() {
        return MultiIdentityStore.class;
    }

    @Override
    public AbstractAction createAction(DataStoreEntryRef<MultiIdentityStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public String getId() {
        return "convertMultiIdentity";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<MultiIdentityStore> {

        @Override
        public void executeImpl() {
            var counter = new AtomicInteger(0);
            var l = ref.getStore().getAvailableIdentities();
            syncLocal(counter, l);
        }

        private void syncLocal(AtomicInteger counter, List<DataStoreEntryRef<IdentityStore>> list) {
            var current = list.get(counter.get());
            if (current.getStore() instanceof LocalIdentityStore) {
                IdentityConvert.syncLocal(current.asNeeded(), true, ignored -> {
                    var next = counter.incrementAndGet();
                    if (next < list.size()) {
                        syncLocal(counter, list);
                    } else {
                        syncMulti();
                    }
                });
            } else {
                var next = counter.incrementAndGet();
                if (next < list.size()) {
                    syncLocal(counter, list);
                } else {
                    syncMulti();
                }
            }
        }

        private void syncMulti() {
            var cat = DataStorage.get()
                    .getStoreCategoryIfPresent(ref.get().getCategoryUuid())
                    .orElseThrow();
            var inSynced = DataStorage.get().getCategoryParentHierarchy(cat).stream()
                    .anyMatch(dataStoreCategory ->
                            dataStoreCategory.getUuid().equals(DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID));
            if (!inSynced) {
                DataStorage.get()
                        .moveEntryToCategory(
                                ref.get(),
                                DataStorage.get()
                                        .getStoreCategoryIfPresent(DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID)
                                        .orElseThrow());
            }
        }
    }
}
