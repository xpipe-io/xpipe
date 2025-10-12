package io.xpipe.app.hub.action.impl;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.FileSystemStore;
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

public class BrowseHubLeafProvider implements HubLeafProvider<FileSystemStore> {

    @Override
    public Action createAction(DataStoreEntryRef<FileSystemStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.OPEN;
    }

    @Override
    public boolean isMajor() {
        return true;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<FileSystemStore> store) {
        return AppI18n.observable("browseFiles");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<FileSystemStore> store) {
        return new LabelGraphic.IconGraphic("mdi2f-folder-open-outline");
    }

    @Override
    public Class<FileSystemStore> getApplicableClass() {
        return FileSystemStore.class;
    }

    @Override
    public String getId() {
        return "browseStore";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<FileSystemStore> {

        FilePath path;

        @Override
        public void executeImpl() throws Exception {
            DataStoreEntryRef<FileSystemStore> replacement =
                    ProcessControlProvider.get().replace(ref);
            BrowserFullSessionModel.DEFAULT.openFileSystemSync(
                    replacement, null, (m) -> path, new SimpleBooleanProperty(), true);
            AppLayoutModel.get().selectBrowser();
        }
    }
}
