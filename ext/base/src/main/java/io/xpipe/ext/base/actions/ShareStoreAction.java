package io.xpipe.ext.base.actions;

import io.xpipe.app.core.AppActionLinkDetector;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.SecretValue;
import javafx.beans.value.ObservableValue;
import lombok.Value;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class ShareStoreAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        DataStore store;

        @Override
        public boolean requiresPlatform() {
            return false;
        }

        public static String create(DataStore store) {
            return "xpipe://add/store/" + SecretValue.encrypt(store.toString()).getEncryptedValue();
        }

        @Override
        public void execute() throws Exception {
            var string = create(store);
            var selection = new StringSelection(string);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            AppActionLinkDetector.setLastDetectedAction(string);
            clipboard.setContents(selection, selection);
        }
    }

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<DataStore>() {

            @Override
            public ActionProvider.Action createAction(DataStore store) {
                return new Action(store);
            }

            @Override
            public Class<DataStore> getApplicableClass() {
                return DataStore.class;
            }

            @Override
            public boolean isApplicable(DataStore o) throws Exception {
                return DataStoreProviders.byStore(o).isShareable();
            }

            @Override
            public ObservableValue<String> getName(DataStore store) {
                return AppI18n.observable("base.copyShareLink");
            }

            @Override
            public String getIcon(DataStore store) {
                return "mdi2c-clipboard-list-outline";
            }
        };
    }
}
