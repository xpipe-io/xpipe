package io.xpipe.ext.base.identity;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.hub.comp.StoreCreationModel;
import io.xpipe.app.hub.comp.StoreListChoiceComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.*;

import io.xpipe.app.util.ObservableSubscriber;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
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
        var exclusive = new SimpleObjectProperty<>(st.getExclusive());
        var perUser = new SimpleObjectProperty<>(st.getPerUser());

        var listUpdate = new ObservableSubscriber();
        var selected = new SimpleObjectProperty<DataStoreEntryRef<IdentityStore>>(st.getSelected().orElse(null));
        identities.addListener((observable, oldValue, newValue) -> {
            var hasActive = identities.contains(selected.get());
            if (!hasActive) {
                selected.set(identities.stream().filter(ref -> ref.get().getValidity().isUsable()).findFirst().orElse(null));
                listUpdate.trigger();
            }
        });

        var choice = new StoreListChoiceComp<>(identities, IdentityStore.class,
                ref -> !(ref.get().equals(model.getExistingEntry())) && !identities.contains(ref) && !MultiIdentityStore.isExclusivelyHeld(ref),
                StoreViewState.get().getAllIdentitiesCategory(),
                DataStoreCreationCategory.IDENTITY) {

            @Override
            protected ObservableValue<String> getName(DataStoreEntryRef<IdentityStore> ref) {
                var labelName = Bindings.createStringBinding(() -> {
                    var base = ref.get().getName();
                    var active = ref.equals(selected.get());
                    return base + (active ? " (" + AppI18n.get("active") + ")" : "");
                }, selectedList, AppI18n.activeLanguage(), listUpdate);
                return labelName;
            }

            @Override
            protected BaseRegionBuilder<?, ?> buildCustomButtons(DataStoreEntryRef<IdentityStore> ref) {
                var select = new IconButtonComp("mdi2i-image-filter-center-focus", () -> {
                    st.select(ref);
                    selected.set(ref);
                    listUpdate.trigger();
                });
                select.disable(ref.get().getProvider() == null);
                select.describe(d -> d.nameKey("makeActive"));
                return new HorizontalComp(List.of(select, RegionBuilder.hspacer(5)));
            }
        };
        var options = new OptionsBuilder()
                .nameAndDescription("multiIdentityList")
                .addComp(choice,
                        identities)
                .nameAndDescription("multiIdentityExclusive")
                .addToggle(exclusive)
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
                                        .exclusive(exclusive.get())
                                        .build();
                            }

                            var all = new ArrayList<UUID>();
                            for (DataStoreEntryRef<IdentityStore> identity : identities) {
                                all.add(identity.get().getUuid());
                            }

                            return MultiIdentityStore.builder()
                                    .identities(all)
                                    .exclusive(exclusive.get())
                                    .perUser(perUser.get())
                                    .build();
                        },
                        store);
        var dialog = new GuiDialog(options, entry -> {
            var finalStore = (MultiIdentityStore) entry.getStore();
            finalStore.select(selected.get());
        });
        return dialog;
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
