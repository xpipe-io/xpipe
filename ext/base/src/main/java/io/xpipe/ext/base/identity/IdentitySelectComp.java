package io.xpipe.ext.base.identity;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ComboTextFieldComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.store.StoreCreationComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.EncryptedValue;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.SecretRetrievalStrategy;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

import atlantafx.base.theme.Styles;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class IdentitySelectComp extends Comp<CompStructure<HBox>> {

    public IdentitySelectComp(
            ObjectProperty<DataStoreEntryRef<IdentityStore>> selectedReference,
            Property<String> inPlaceUser,
            ObservableValue<SecretRetrievalStrategy> password,
            ObservableValue<SshIdentityStrategy> identityStrategy,
            boolean allowUserInput) {
        this.selectedReference = selectedReference;
        this.inPlaceUser = inPlaceUser;
        this.password = password;
        this.identityStrategy = identityStrategy;
        this.allowUserInput = allowUserInput;
    }

    private final ObjectProperty<DataStoreEntryRef<IdentityStore>> selectedReference;
    private final Property<String> inPlaceUser;
    private final ObservableValue<SecretRetrievalStrategy> password;
    private final ObservableValue<SshIdentityStrategy> identityStrategy;
    private final boolean allowUserInput;

    private void addNamedIdentity() {
        var canSync = DataStorage.get()
                .getStoreCategoryIfPresent(DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID)
                .isPresent();
        var id = canSync
                ? SyncedIdentityStore.builder()
                        .username(inPlaceUser.getValue())
                        .password(EncryptedValue.VaultKey.of(password.getValue()))
                        .sshIdentity(EncryptedValue.VaultKey.of(identityStrategy.getValue()))
                        .build()
                : LocalIdentityStore.builder()
                        .username(inPlaceUser.getValue())
                        .password(EncryptedValue.CurrentKey.of(password.getValue()))
                        .sshIdentity(EncryptedValue.CurrentKey.of(identityStrategy.getValue()))
                        .build();
        StoreCreationComp.showCreation(
                id,
                DataStoreCreationCategory.IDENTITY,
                dataStoreEntry -> {
                    PlatformThread.runLaterIfNeeded(() -> {
                        applyRef(dataStoreEntry.ref());
                    });
                },
                false);
    }

    private void editNamedIdentity() {
        var id = selectedReference.get();
        if (id == null) {
            return;
        }

        StoreCreationComp.showEdit(id.get());
    }

    @Override
    public CompStructure<HBox> createBase() {
        ObservableValue<LabelGraphic> icon = Bindings.createObjectBinding(
                () -> {
                    return selectedReference.get() != null
                            ? new LabelGraphic.IconGraphic("mdi2a-account-edit")
                            : new LabelGraphic.IconGraphic("mdi2a-account-multiple-plus");
                },
                selectedReference);
        var addButton = new ButtonComp(null, icon, () -> {
            if (selectedReference.get() != null) {
                editNamedIdentity();
            } else {
                addNamedIdentity();
            }
        });
        addButton.styleClass(Styles.RIGHT_PILL).grow(false, true).tooltipKey("addReusableIdentity");

        var nodes = new ArrayList<Comp<?>>();
        nodes.add(createComboBox());
        nodes.add(addButton);
        var layout = new HorizontalComp(nodes).apply(struc -> struc.get().setFillHeight(true));

        layout.apply(struc -> {
            struc.get().focusedProperty().addListener((observable, oldValue, newValue) -> {
                struc.get().getChildren().getFirst().requestFocus();
            });
        });

        var structure = layout.createStructure();
        return new SimpleCompStructure<>(structure.get());
    }

    private String formatName(DataStoreEntry storeEntry) {
        IdentityStore id = storeEntry.getStore().asNeeded();
        var suffix = id instanceof LocalIdentityStore
                ? AppI18n.get("localIdentity")
                : id instanceof SyncedIdentityStore && storeEntry.isPerUserStore()
                        ? AppI18n.get("userIdentity")
                        : AppI18n.get("globalIdentity");
        return storeEntry.getName() + " (" + suffix + ")";
    }

    private void applyRef(DataStoreEntryRef<IdentityStore> newRef) {
        this.selectedReference.setValue(newRef);
    }

    private Comp<?> createComboBox() {
        var map = new LinkedHashMap<String, DataStoreEntryRef<IdentityStore>>();
        for (DataStoreEntry storeEntry : DataStorage.get().getStoreEntries()) {
            if (storeEntry.getValidity().isUsable() && storeEntry.getStore() instanceof IdentityStore) {
                map.put(formatName(storeEntry), storeEntry.ref());
            }
        }

        StoreViewState.get().getAllEntries().getList().addListener((ListChangeListener<? super StoreEntryWrapper>)
                c -> {
                    map.clear();
                    for (DataStoreEntry storeEntry : DataStorage.get().getStoreEntries()) {
                        if (storeEntry.getValidity().isUsable() && storeEntry.getStore() instanceof IdentityStore) {
                            map.put(formatName(storeEntry), storeEntry.ref());
                        }
                    }
                });

        var prop = new SimpleStringProperty();
        if (inPlaceUser.getValue() != null) {
            prop.setValue(inPlaceUser.getValue());
        } else if (selectedReference.getValue() != null) {
            prop.setValue(formatName(selectedReference.getValue().get()));
        }

        prop.addListener((observable, oldValue, newValue) -> {
            var ex = map.get(newValue);
            applyRef(ex);

            if (ex == null) {
                inPlaceUser.setValue(newValue);
            } else {
                inPlaceUser.setValue(null);
            }
        });

        selectedReference.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                PlatformThread.runLaterIfNeeded(() -> {
                    var s = formatName(newValue.get());
                    prop.setValue(s);
                });
            }
        });

        var combo = new ComboTextFieldComp(prop, map.keySet().stream().toList(), param -> {
            return new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        return;
                    }

                    setText(item);
                }
            };
        });
        combo.apply(struc -> struc.get().setEditable(allowUserInput));
        combo.hgrow();
        combo.styleClass(Styles.LEFT_PILL);
        combo.grow(false, true);
        combo.apply(struc -> {
            var binding = Bindings.createStringBinding(
                    () -> {
                        if (selectedReference.get() != null) {
                            return selectedReference.get().get().getName();
                        }

                        return AppI18n.get("defineNewIdentityOrSelect");
                    },
                    AppPrefs.get().language(),
                    selectedReference);
            struc.get().promptTextProperty().bind(binding);
        });
        combo.apply(struc -> {
            struc.get().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE && !allowUserInput) {
                    selectedReference.setValue(null);
                    prop.setValue(null);
                    event.consume();
                }
            });
        });
        return combo;
    }
}
