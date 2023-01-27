package io.xpipe.ext.base.actions;

import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.SecretValue;
import io.xpipe.extension.DataStoreActionProvider;
import io.xpipe.extension.DataStoreProviders;
import io.xpipe.extension.I18n;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;

import java.util.Map;

public class ShareStoreAction implements DataStoreActionProvider<DataStore> {

    @Override
    public boolean showIfDisabled() {
        return false;
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
        return I18n.observable("base.copyShareLink");
    }

    @Override
    public String getIcon(DataStore store) {
        return "mdi2c-clipboard-list-outline";
    }

    public static String create(DataStore store) {
        return "xpipe://add/store/" + SecretValue.encrypt(store.toString()).getEncryptedValue();
    }

    @Override
    public void execute(DataStore store) throws Exception {
        var string = create(store);
        Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, string, DataFormat.URL, string));
    }
}
