package io.xpipe.ext.base.script;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.FileSystemStore;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.DesktopHelper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class ScriptSourceBrowseActionProvider implements HubLeafProvider<ScriptSourceStore> {

    @Override
    public AbstractAction createAction(DataStoreEntryRef<ScriptSourceStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public boolean isMajor() {
        return true;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<ScriptSourceStore> store) {
        return AppI18n.observable("browse");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<ScriptSourceStore> store) {
        return new LabelGraphic.IconGraphic("mdi2f-folder-search-outline");
    }

    @Override
    public Class<ScriptSourceStore> getApplicableClass() {
        return ScriptSourceStore.class;
    }

    @Override
    public String getId() {
        return "browseScriptSource";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<ScriptSourceStore> {

        @Override
        public void executeImpl() {
            DesktopHelper.browseFile(ref.getStore().getSource().getLocalPath());
        }
    }
}
