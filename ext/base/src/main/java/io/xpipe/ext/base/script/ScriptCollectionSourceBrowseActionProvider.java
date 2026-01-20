package io.xpipe.ext.base.script;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.DesktopHelper;
import javafx.beans.value.ObservableValue;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class ScriptCollectionSourceBrowseActionProvider implements HubLeafProvider<ScriptCollectionSourceStore> {

    @Override
    public AbstractAction createAction(DataStoreEntryRef<ScriptCollectionSourceStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public boolean isMajor() {
        return true;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<ScriptCollectionSourceStore> store) {
        return AppI18n.observable("browse");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<ScriptCollectionSourceStore> store) {
        return new LabelGraphic.IconGraphic("mdi2f-folder-search-outline");
    }

    @Override
    public Class<ScriptCollectionSourceStore> getApplicableClass() {
        return ScriptCollectionSourceStore.class;
    }

    @Override
    public String getId() {
        return "browseScriptCollectionSource";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<ScriptCollectionSourceStore> {

        @Override
        public void executeImpl() {
            DesktopHelper.browseFile(ref.getStore().getSource().getLocalPath());
        }
    }
}
