package io.xpipe.app.hub.action.impl;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.StartOnInitStore;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.FilePath;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class StartOnInitHubLeafProvider implements HubLeafProvider<StartOnInitStore> {

    @Override
    public Action createAction(DataStoreEntryRef<StartOnInitStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<StartOnInitStore> store) {
        return AppI18n.observable(store.getStore().isEnabled() ? "disableStartOnInit" : "enableStartOnInit");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<StartOnInitStore> store) {
        return new LabelGraphic.IconGraphic(store.getStore().isEnabled() ? "mdi2t-toggle-switch-off-outline" : "mdi2t-toggle-switch-outline");
    }

    @Override
    public Class<StartOnInitStore> getApplicableClass() {
        return StartOnInitStore.class;
    }

    @Override
    public String getId() {
        return "toggleStartOnInit";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<StartOnInitStore> {

        @Override
        public void executeImpl() throws Exception {
            if (ref.getStore().isEnabled()) {
                ref.getStore().disable();
            } else  {
                ref.getStore().enable();
            }
        }
    }
}
