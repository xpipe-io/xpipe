package io.xpipe.ext.base.actions;

import io.xpipe.app.util.ExternalEditor;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.store.DataFlow;
import io.xpipe.extension.DataStoreActionProvider;
import io.xpipe.extension.I18n;
import javafx.beans.value.ObservableValue;

public class FileEditAction implements DataStoreActionProvider<FileStore> {

    @Override
    public Class<FileStore> getApplicableClass() {
        return FileStore.class;
    }

    @Override
    public boolean isApplicable(FileStore o) throws Exception {
        return o.getFlow().equals(DataFlow.INPUT_OUTPUT);
    }

    @Override
    public ObservableValue<String> getName(FileStore store) {
        return I18n.observable("base.editFile");
    }

    @Override
    public String getIcon(FileStore store) {
        return "mdal-edit";
    }

    @Override
    public void execute(FileStore store) throws Exception {
        if (store.getFileSystem().equals(new LocalStore())) {
            ExternalEditor.get().openInEditor(store.getFile());
        } else {
            ExternalEditor.get()
                    .startEditing(store.getFileName(), store.getFileExtension(), store, () -> store.openInput(), () -> store.openOutput());
        }
    }
}
