package io.xpipe.ext.base.identity;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.hub.comp.StoreCreationModel;
import io.xpipe.app.hub.comp.StoreListChoiceComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.*;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MultiIdentityStoreProvider extends IdentityStoreProvider {

    @Override
    public boolean allowCreation() {
        return DataStorage.get().getStoreEntries().stream().anyMatch(e -> e.getStore() instanceof IdentityStore);
    }

    @Override
    public GuiDialog guiDialog(StoreCreationModel model, Property<DataStore> store) {
        MultiIdentityStore st = (MultiIdentityStore) store.getValue();

        var initialAvailableIdentities = st.getAvailableIdentities();
        var identities = new SimpleListProperty<DataStoreEntryRef<IdentityStore>>(FXCollections.observableArrayList());
        for (UUID uuid : st.getIdentities()) {
            var available = initialAvailableIdentities.stream()
                    .filter(id -> id.get().getUuid().equals(uuid))
                    .findFirst();
            if (available.isPresent()) {
                identities.add(available.get());
            } else {
                identities.add(new DataStoreEntryRef<>(DataStoreEntry.createNew(
                        uuid, DataStorage.DEFAULT_CATEGORY_UUID, AppI18n.get("unknown"), null)));
            }
        }
        var perUser = new SimpleBooleanProperty(st.isPerUser());

        return new OptionsBuilder()
                .nameAndDescription("multiIdentityList")
                .addComp(
                        new StoreListChoiceComp<>(
                                identities,
                                IdentityStore.class,
                                ref -> !(ref.getStore() instanceof MultiIdentityStore) && !identities.contains(ref),
                                StoreViewState.get().getAllIdentitiesCategory(),
                                DataStoreCreationCategory.IDENTITY),
                        identities)
                .nameAndDescription(
                        DataStorageUserHandler.getInstance().getActiveUser() != null
                                ? "identityPerUser"
                                : "identityPerUserDisabled")
                .addToggle(perUser)
                .disable(DataStorageUserHandler.getInstance().getActiveUser() == null)
                .bind(
                        () -> {
                            // User made no changes in GUI
                            if (identities.getValue().stream()
                                    .map(ref -> ref.get().getUuid())
                                    .toList()
                                    .equals(st.getIdentities())) {
                                return MultiIdentityStore.builder()
                                        .identities(st.getIdentities())
                                        .perUser(perUser.get())
                                        .build();
                            }

                            var all = new ArrayList<UUID>();
                            for (DataStoreEntryRef<IdentityStore> identity : identities) {
                                all.add(identity.get().getUuid());
                            }

                            return MultiIdentityStore.builder()
                                    .identities(all)
                                    .perUser(perUser.get())
                                    .build();
                        },
                        store)
                .buildDialog();
    }

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        return MultiIdentityStore.builder().identities(new ArrayList<>()).build();
    }

    @Override
    public String getId() {
        return "multiIdentity";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(MultiIdentityStore.class);
    }
}
