package io.xpipe.ext.base.actions;

import io.xpipe.core.impl.FileStore;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.ActionProvider;
import io.xpipe.extension.util.DesktopHelper;
import javafx.beans.value.ObservableValue;
import lombok.Value;

import java.nio.file.Files;
import java.nio.file.Path;

public class FileBrowseAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        FileStore store;

        @Override
        public boolean requiresPlatform() {
            return false;
        }

        @Override
        public void execute() throws Exception {
            DesktopHelper.browseFileInDirectory(Path.of(store.getFile()));
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
                return o.getFileSystem().equals(new LocalStore()) && Files.exists(Path.of(o.getFile()));
            }

            @Override
            public ObservableValue<String> getName(FileStore store) {
                return I18n.observable("base.browseFile");
            }

            @Override
            public String getIcon(FileStore store) {
                return "mdi2f-folder-open-outline";
            }
        };
    }
}
