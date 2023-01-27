package io.xpipe.ext.base.actions;

import io.xpipe.core.impl.FileStore;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.extension.DataStoreActionProvider;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.OsHelper;
import javafx.beans.value.ObservableValue;

import java.nio.file.Files;
import java.nio.file.Path;

public class FileBrowseAction implements DataStoreActionProvider<FileStore> {

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

    @Override
    public void execute(FileStore store) throws Exception {
        OsHelper.browseFileInDirectory(Path.of(store.getFile()));
    }
}
