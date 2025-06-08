package io.xpipe.ext.base.action;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.action.LeafStoreActionProvider;
import io.xpipe.app.action.StoreAction;
import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.store.FileSystemStore;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;

import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class BrowseStoreActionProvider implements LeafStoreActionProvider<FileSystemStore> {

            @Override
            public Action createAction(DataStoreEntryRef<FileSystemStore> ref) {
                return Action.builder().ref(ref).build();
            }

            @Override
            public Class<FileSystemStore> getApplicableClass() {
                return FileSystemStore.class;
            }

            @Override
            public boolean isMajor(DataStoreEntryRef<FileSystemStore> o) {
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
            BrowserFullSessionModel.DEFAULT.openFileSystemSync(replacement, (m) -> path, new SimpleBooleanProperty(), true);
            AppLayoutModel.get().selectBrowser();
        }
    }
}
