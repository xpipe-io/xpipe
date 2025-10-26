package io.xpipe.ext.base.host;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class AbstractHostCreationActionProvider implements HubLeafProvider<AbstractHostTransformStore> {

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<AbstractHostTransformStore> store) {
        return AppI18n.observable("abstractHostConvert");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<AbstractHostTransformStore> store) {
        return new LabelGraphic.IconGraphic("mdi2c-cog-transfer-outline");
    }

    @Override
    public Class<?> getApplicableClass() {
        return AbstractHostTransformStore.class;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<AbstractHostTransformStore> o) {
        return o.getStore().canConvertToAbstractHost();
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<AbstractHostTransformStore> {

        @Override
        public void executeImpl() {
            var d = ref.getStore();
            var ah = d.createAbstractHostStore();
            var entry = DataStorage.get().addStoreIfNotPresent(ref.get().getName(), ah);
            entry.setExpanded(true);
            var newStore = d.withNewParent(entry.ref());
            DataStorage.get().updateEntryStore(ref.get(), newStore);
            entry.setChildrenCache(null);
            StoreViewState.get().triggerStoreListUpdate();
        }
    }
}
