package io.xpipe.ext.base.identity;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.creation.StoreCreationModel;
import io.xpipe.app.hub.entry.StoreEntryBadge;
import io.xpipe.app.hub.entry.StoreEntryInformation;
import io.xpipe.app.hub.entry.StoreEntryWrapper;
import io.xpipe.app.hub.list.StoreListChoiceComp;
import io.xpipe.app.hub.list.StoreViewState;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.DataStorageAccessType;
import io.xpipe.app.secret.DataStorageAccessHandler;
import io.xpipe.app.storage.*;
import io.xpipe.app.store.DataStore;
import io.xpipe.app.store.DataStoreCreationCategory;
import io.xpipe.app.util.GuiDialog;
import io.xpipe.app.util.ObservableSubscriber;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MultiIdentityStoreProvider extends IdentityStoreProvider {

    @Override
    public UUID getTargetCategory(DataStore store, UUID target) {
        var st = (MultiIdentityStore) store;
        if (st == null) {
            return target;
        }

        var cat = DataStorage.get().getStoreCategoryIfPresent(target).orElseThrow();
        var inSynced = DataStorage.get().getCategoryParentHierarchy(cat).stream()
                .anyMatch(dataStoreCategory ->
                        dataStoreCategory.getUuid().equals(DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID));

        var childrenLocal = st.areAllChildrenLocal();
        var childrenSynced = st.areAllChildrenSynced();

        if (childrenSynced && !inSynced) {
            return DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID;
        } else if (childrenLocal && inSynced) {
            return DataStorage.LOCAL_IDENTITIES_CATEGORY_UUID;
        } else {
            return target;
        }
    }

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
                var foundInaccessible = DataStorage.get().getInaccessibleEntry(uuid);
                if (foundInaccessible.isPresent()) {
                    identities.add(foundInaccessible.get().ref());
                } else {
                    identities.add(new DataStoreEntryRef<>(DataStoreEntry.createNew(
                            uuid, DataStorage.DEFAULT_CATEGORY_UUID, AppI18n.get("unknown"), null)));
                }
            }
        }
        var exclusive = new SimpleObjectProperty<>(st.getExclusive());
        var scope = new SimpleObjectProperty<>(st.getAccessScope());

        var listUpdate = new ObservableSubscriber();
        var selected = new SimpleObjectProperty<>(st.getSelected().orElse(null));
        identities.addListener((observable, oldValue, newValue) -> {
            var hasActive = identities.contains(selected.get());
            if (!hasActive) {
                selected.set(identities.stream()
                        .filter(ref -> ref.get().getValidity().isUsable())
                        .findFirst()
                        .orElse(null));
                listUpdate.trigger();
            }
        });

        var choice =
                new StoreListChoiceComp<>(
                        identities,
                        IdentityStore.class,
                        ref -> !(ref.get().equals(model.getExistingEntry()))
                                && !identities.contains(ref)
                                && !MultiIdentityStore.isExclusivelyHeld(ref),
                        StoreViewState.get().getAllIdentitiesCategory(),
                        DataStoreCreationCategory.IDENTITY) {

                    @Override
                    protected ObservableValue<String> getName(DataStoreEntryRef<IdentityStore> ref) {
                        var labelName = Bindings.createStringBinding(
                                () -> {
                                    var base = ref.get().getName();
                                    var active = ref.equals(selected.get());
                                    var inaccessible =
                                            !DataStorage.get().getStoreEntries().contains(ref.get());
                                    var suffix = active
                                            ? " (" + AppI18n.get("active") + ")"
                                            : inaccessible ? " (" + AppI18n.get("inaccessible") + ")" : "";
                                    return base + suffix;
                                },
                                selectedList,
                                AppI18n.activeLanguage(),
                                listUpdate);
                        return labelName;
                    }

                    @Override
                    protected BaseRegionBuilder<?, ?> buildCustomButtons(DataStoreEntryRef<IdentityStore> ref) {
                        var select = new IconButtonComp("mdi2c-check", () -> {
                            st.select(ref);
                            selected.set(ref);
                            listUpdate.trigger();
                        });
                        var inaccessible = !DataStorage.get().getStoreEntries().contains(ref.get());
                        select.disable(ref.get().getProvider() == null || inaccessible);
                        select.hide(selected.isEqualTo(ref));
                        select.describe(d -> d.nameKey("makeActive"));
                        return new HorizontalComp(List.of(select, RegionBuilder.hspacer(5)));
                    }
                };
        var roleBased = DataStorageAccessHandler.getInstance().isAccessRestricted()
                && DataStorageAccessHandler.getInstance().getType() == DataStorageAccessType.ROLE;
        var options = new OptionsBuilder()
                .nameAndDescription("multiIdentityList")
                .addComp(choice, identities)
                .nameAndDescription("multiIdentityExclusive")
                .addToggle(exclusive)
                .nameAndDescription(roleBased ? "identityPerRole" : "identityPerRoleDisabled")
                .addComp(new DataStoreAccessScopeComp(scope), scope)
                .nonNull()
                .bind(
                        () -> {
                            // User made no changes in GUI
                            if (identities.getValue().stream()
                                    .map(ref -> ref.get().getUuid())
                                    .toList()
                                    .equals(st.getIdentities())) {
                                return MultiIdentityStore.builder()
                                        .identities(st.getIdentities())
                                        .accessScope(scope.get())
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
                                    .accessScope(scope.get())
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

    @Override
    public String summaryString(StoreEntryWrapper wrapper) {
        var cat = DataStorage.get()
                .getStoreCategoryIfPresent(wrapper.getEntry().getCategoryUuid())
                .orElseThrow();
        var inSynced = DataStorage.get().getCategoryParentHierarchy(cat).stream()
                .anyMatch(dataStoreCategory ->
                        dataStoreCategory.getUuid().equals(DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID));
        return (inSynced ? AppI18n.get("syncedMultiIdentity") : AppI18n.get("localMultiIdentity"));
    }

    @Override
    public StoreEntryInformation buildInformation(StoreSection section) {
        var st = (MultiIdentityStore) section.getWrapper().getStore().getValue();
        var active = st.getSelected().orElse(null);
        if (active == null) {
            return StoreEntryInformation.of(StoreEntryBadge.ofFailure("None"));
        }

        var selection = StoreEntryBadge.ofSetting(active.get().getName());
        selection = selection != null
                ? selection.withAction(StoreEntryBadge.Action.providerMenu("multiIdentitySwitch"))
                : null;
        return StoreEntryInformation.of(selection);
    }

    @Override
    public boolean showIncompleteInfo() {
        return true;
    }
}
