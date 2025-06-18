package io.xpipe.ext.base.identity;

import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.store.DataStore;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;

import java.util.List;

public class PasswordManagerIdentityStoreProvider extends IdentityStoreProvider {

    @Override
    public DataStoreCreationCategory getCreationCategory() {
        return AppPrefs.get().passwordManager().getValue() != null ? DataStoreCreationCategory.IDENTITY : null;
    }

    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        PasswordManagerIdentityStore st = (PasswordManagerIdentityStore) store.getValue();

        var key = new SimpleStringProperty(st.getKey());

        var comp = new PasswordManagerTestComp(key, false);
        return new OptionsBuilder()
                .nameAndDescription("passwordManagerKey")
                .addComp(comp.hgrow(), key)
                .nonNull()
                .bind(
                        () -> {
                            return PasswordManagerIdentityStore.builder()
                                    .key(key.get())
                                    .build();
                        },
                        store)
                .buildDialog();
    }

    @Override
    public String getId() {
        return "passwordManagerIdentity";
    }

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        return PasswordManagerIdentityStore.builder().key(null).build();
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(PasswordManagerIdentityStore.class);
    }
}
