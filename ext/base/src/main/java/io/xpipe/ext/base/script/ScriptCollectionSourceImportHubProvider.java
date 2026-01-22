package io.xpipe.ext.base.script;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStoreEntryRef;
import javafx.beans.value.ObservableValue;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class ScriptCollectionSourceImportHubProvider implements HubLeafProvider<ScriptCollectionSourceStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Override
    public boolean isMajor() {
        return true;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<ScriptCollectionSourceStore> store) {
        return AppI18n.observable("importScripts");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<ScriptCollectionSourceStore> store) {
        return new LabelGraphic.IconGraphic("mdi2i-import");
    }

    @Override
    public Class<?> getApplicableClass() {
        return ScriptCollectionSourceStore.class;
    }

    @Override
    public String getId() {
        return "importScriptCollection";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<ScriptCollectionSourceStore> {

        @Override
        public void executeImpl() {
            var dialog = new ScriptCollectionSourceImportDialog(ref.getStore().getSource());
            dialog.show();
        }
    }
}
