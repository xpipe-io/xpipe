package io.xpipe.ext.base.actions;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.util.ExternalEditor;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.store.DataFlow;
import javafx.beans.value.ObservableValue;
import lombok.Value;

public class FileEditAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        FileStore store;

        @Override
        public boolean requiresPlatform() {
            return false;
        }

        @Override
        public void execute() throws Exception {
            if (store.getFileSystem().equals(new LocalStore())) {
                ExternalEditor.get().openInEditor(store.getFile());
            } else {
                ExternalEditor.get()
                        .startEditing(
                                store.getFileName(),
                                store.getFileExtension(),
                                store,
                                () -> store.openInput(),
                                () -> store.openOutput());
            }
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
                return o.getFlow().equals(DataFlow.INPUT_OUTPUT);
            }

            @Override
            public ObservableValue<String> getName(FileStore store) {
                return AppI18n.observable("base.editFile");
            }

            @Override
            public String getIcon(FileStore store) {
                return "mdal-edit";
            }
        };
    }
}
