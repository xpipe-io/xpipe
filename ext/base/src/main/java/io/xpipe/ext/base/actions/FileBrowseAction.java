package io.xpipe.ext.base.actions;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.impl.LocalStore;
import javafx.beans.value.ObservableValue;
import lombok.Value;

import java.nio.file.Files;
import java.nio.file.Path;

public class FileBrowseAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        FileStore store;

        @Override
        public boolean requiresJavaFXPlatform() {
            return false;
        }

        @Override
        public void execute() throws Exception {
            DesktopHelper.browseFileInDirectory(Path.of(store.getPath()));
        }
    }

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<FileStore>() {

            @Override
            public ActionProvider.Action createAction(FileStore store) {
                return new Action(store);
            }

            @Override
            public Class<FileStore> getApplicableClass() {
                return FileStore.class;
            }

            @Override
            public boolean isApplicable(FileStore o) throws Exception {
                return o.getFileSystem().equals(new LocalStore()) && Files.exists(Path.of(o.getPath()));
            }

            @Override
            public ObservableValue<String> getName(FileStore store) {
                return AppI18n.observable("base.browseFile");
            }

            @Override
            public String getIcon(FileStore store) {
                return "mdi2f-folder-open-outline";
            }
        };
    }
}
