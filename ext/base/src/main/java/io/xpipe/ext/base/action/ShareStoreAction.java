package io.xpipe.ext.base.action;

import io.xpipe.app.core.AppActionLinkDetector;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.SecretHelper;
import io.xpipe.core.store.DataStore;
import javafx.beans.value.ObservableValue;
import lombok.Value;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class ShareStoreAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry store;

        @Override
        public boolean requiresJavaFXPlatform() {
            return false;
        }

        public static String create(DataStore store) {
            return "xpipe://addStore/"
                    + SecretHelper.encryptInPlace(store.toString()).getEncryptedValue();
        }

        @Override
        public void execute() {
            var string = create(store.getStore());
            var selection = new StringSelection(string);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            AppActionLinkDetector.setLastDetectedAction(string);
            clipboard.setContents(selection, selection);
        }
    }

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<DataStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<DataStore> getApplicableClass() {
                return DataStore.class;
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<DataStore> o) {
                return o.get().getProvider().isShareable();
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<DataStore> store) {
                return AppI18n.observable("base.copyShareLink");
            }

            @Override
            public String getIcon(DataStoreEntryRef<DataStore> store) {
                return "mdi2c-clipboard-list-outline";
            }
        };
    }
}
