package io.xpipe.ext.base.service;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.BatchHubProvider;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class ServiceRefreshActionProvider
        implements HubLeafProvider<FixedServiceCreatorStore>, BatchHubProvider<FixedServiceCreatorStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Override
    public boolean isMajor(DataStoreEntryRef<FixedServiceCreatorStore> o) {
        return true;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<FixedServiceCreatorStore> store) {
        return AppI18n.observable("refreshServices");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<FixedServiceCreatorStore> store) {
        return new LabelGraphic.IconGraphic("mdi2w-web");
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<FixedServiceCreatorStore> o) {
        return o.getStore().allowManualServicesRefresh();
    }

    @Override
    public ObservableValue<String> getName() {
        return AppI18n.observable("refreshServices");
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2w-web");
    }

    @Override
    public Class<?> getApplicableClass() {
        return FixedServiceCreatorStore.class;
    }

    @Override
    public Action createBatchAction(DataStoreEntryRef<FixedServiceCreatorStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public String getId() {
        return "refreshServices";
    }

    @Jacksonized
    @SuperBuilder
    static class Action extends StoreAction<FixedServiceCreatorStore> {

        @Override
        public void executeImpl() {
            ref.get().setExpanded(true);
            var e = DataStorage.get()
                    .addStoreIfNotPresent(
                            "Services",
                            FixedServiceGroupStore.builder().parent(ref).build());
            DataStorage.get().refreshChildren(e);
        }
    }
}
