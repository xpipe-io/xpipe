package io.xpipe.ext.base.identity;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.cred.PasswordManagerAgentStrategy;
import io.xpipe.app.cred.SshIdentityStrategyChoiceConfig;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.hub.comp.StoreEntryWrapper;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageUserHandler;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;

import java.util.List;
import java.util.UUID;

public class PasswordManagerIdentityStoreProvider extends IdentityStoreProvider {

    @Override
    public UUID getTargetCategory(DataStore store, UUID target) {
        PasswordManagerIdentityStore st = (PasswordManagerIdentityStore) store;
        if (st == null || !st.isPerUser()) {
            return target;
        }

        var cat = DataStorage.get().getStoreCategoryIfPresent(target).orElseThrow();
        var inSynced = DataStorage.get().getCategoryParentHierarchy(cat).stream()
                .anyMatch(dataStoreCategory ->
                        dataStoreCategory.getUuid().equals(DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID));
        return inSynced ? target : DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID;
    }

    @Override
    public boolean allowCreation() {
        return AppPrefs.get().passwordManager().getValue() != null;
    }

    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        PasswordManagerIdentityStore st = (PasswordManagerIdentityStore) store.getValue();

        var key = new SimpleStringProperty(st.getKey());
        var sshKey = new SimpleObjectProperty<>(st.getSshKey());
        var perUser = new SimpleBooleanProperty(st.isPerUser());

        var sshIdentityChoiceConfig = SshIdentityStrategyChoiceConfig.builder()
                .allowAgentForward(true)
                .allowKeyFileSync(true)
                .perUserKeyFileCheck(() -> false)
                .fileSystem(
                        new ReadOnlyObjectWrapper<>(DataStorage.get().local().ref()))
                .build();
        var sshKeyChoice = OptionsChoiceBuilder.builder()
                .allowNull(true)
                .customConfiguration(sshIdentityChoiceConfig)
                .available(List.of(PasswordManagerAgentStrategy.class))
                .property(sshKey)
                .build();
        var hideSshKeyChoice = Bindings.createBooleanBinding(
                () -> {
                    var pwman = AppPrefs.get().passwordManager().getValue();
                    var strat = pwman.getKeyConfiguration();
                    return strat.useInline() && sshKey.get() == null;
                },
                AppPrefs.get().passwordManager(),
                sshKey);

        var testComp = new PasswordManagerTestComp(key, false);
        return new OptionsBuilder()
                .nameAndDescription("passwordManagerKey")
                .addComp(testComp.hgrow(), key)
                .nonNull()
                .nameAndDescription("passwordManagerAdditionalKey")
                .sub(sshKeyChoice.build(), sshKey)
                .hide(hideSshKeyChoice)
                .nameAndDescription(
                        DataStorageUserHandler.getInstance().getActiveUser() != null
                                ? "identityPerUser"
                                : "identityPerUserDisabled")
                .addToggle(perUser)
                .disable(DataStorageUserHandler.getInstance().getActiveUser() == null)
                .bind(
                        () -> {
                            return PasswordManagerIdentityStore.builder()
                                    .key(key.get())
                                    .sshKey(sshKey.get())
                                    .perUser(perUser.get())
                                    .build();
                        },
                        store)
                .buildDialog();
    }

    @Override
    public String summaryString(StoreEntryWrapper wrapper) {
        return wrapper.getEntry().isPerUserStore()
                ? AppI18n.get("userPasswordManagerIdentity")
                : AppI18n.get("globalPasswordManagerIdentity");
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
