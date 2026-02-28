package io.xpipe.ext.base.identity;

import io.xpipe.app.cred.PasswordManagerAgentStrategy;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.pwman.PasswordManagerKeyConfiguration;
import io.xpipe.app.storage.DataStorageUserHandler;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
        var sshKey = new SimpleObjectProperty<>(st.getSshKey());
        var perUser = new SimpleBooleanProperty(st.isPerUser());

        var sshKeyChoice = OptionsChoiceBuilder.builder().allowNull(true)
                .available(List.of(PasswordManagerAgentStrategy.class)).property(sshKey).build();
        var hideSshKeyChoice = Bindings.createBooleanBinding(() -> {
            var pwman = AppPrefs.get().passwordManager().getValue();
            var strat = pwman.getKeyStrategy();
            return !strat.supportsAgent() ||
                    (strat.supportsJoinedEntries() && strat.supportsInlineSshKeys());
        });

        var comp = new PasswordManagerTestComp(key, false);
        return new OptionsBuilder()
                .nameAndDescription("passwordManagerKey")
                .addComp(comp.hgrow(), key)
                .nonNull()
                .nameAndDescription("sshKey")
                .sub(sshKeyChoice.build(), sshKey)
                .hide(hideSshKeyChoice)
                .nameAndDescription(
                        DataStorageUserHandler.getInstance().getActiveUser() != null
                                ? "identityPerUser"
                                : "identityPerUserDisabled")
                .addToggle(perUser)
                .hide(DataStorageUserHandler.getInstance().getActiveUser() == null)
                .bind(
                        () -> {
                            return PasswordManagerIdentityStore.builder()
                                    .key(key.get())
                                    .sshKey(sshKey.get())
                                    .build();
                        },
                        store)
                .buildDialog();
    }

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        return PasswordManagerIdentityStore.builder().key(null).build();
    }

    @Override
    public String getId() {
        return "passwordManagerIdentity";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(PasswordManagerIdentityStore.class);
    }
}
