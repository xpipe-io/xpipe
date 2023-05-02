package io.xpipe.ext.base.actions;

import io.xpipe.app.browser.StandaloneFileBrowser;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.store.StreamDataStore;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import lombok.Value;

import java.io.InputStream;
import java.io.OutputStream;

public class StreamExportAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        StreamDataStore store;

        @Override
        public boolean requiresJavaFXPlatform() {
            return true;
        }

        @Override
        public void execute() throws Exception {
            var outputFile = new SimpleObjectProperty<FileStore>();
            StandaloneFileBrowser.saveSingleFile(outputFile);
            if (outputFile.get() == null) {
                return;
            }

            ThreadHelper.runAsync(() -> {
                try (InputStream inputStream = store.openInput()) {
                    try (OutputStream outputStream = outputFile.get().openOutput()) {
                        inputStream.transferTo(outputStream);
                    }
                } catch (Exception e) {
                    ErrorEvent.fromThrowable(e).handle();
                }
            });
        }
    }

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<StreamDataStore>() {

            @Override
            public Action createAction(StreamDataStore store) {
                return new Action(store);
            }

            @Override
            public boolean isApplicable(StreamDataStore o) throws Exception {
                return o.getFlow() != null && o.getFlow().hasInput();
            }

            @Override
            public Class<StreamDataStore> getApplicableClass() {
                return StreamDataStore.class;
            }

            @Override
            public ObservableValue<String> getName(StreamDataStore store) {
                return AppI18n.observable("base.exportStream");
            }

            @Override
            public String getIcon(StreamDataStore store) {
                return "mdi2f-file-export-outline";
            }
        };
    }
}
