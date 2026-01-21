package io.xpipe.ext.base.identity;




import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.hub.comp.*;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.MenuHelper;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.secret.EncryptedValue;
import io.xpipe.app.secret.SecretNoneStrategy;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.ext.base.identity.ssh.NoIdentityStrategy;
import io.xpipe.ext.base.identity.ssh.SshIdentityStrategy;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.scene.control.ListCell;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class IdentitySelectComp extends RegionBuilder<HBox> {

    private final ObjectProperty<DataStoreEntryRef<IdentityStore>> selectedReference;
    private final Property<String> inPlaceUser;
    private final ObservableValue<SecretRetrievalStrategy> password;
    private final ObservableValue<SshIdentityStrategy> identityStrategy;
    private final boolean allowUserInput;

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

    private void addNamedIdentity() {
        var hasPwMan = AppPrefs.get().passwordManager().getValue() != null;
        var pwManIdentity = DataStorage.get().getStoreEntries().stream()
                .map(entry -> entry.getStore() instanceof PasswordManagerIdentityStore p ? p : null)
                .filter(s -> s != null)
                .findFirst();
        var hasPassword = password.getValue() != null && !(password.getValue() instanceof SecretNoneStrategy);
        var hasSshIdentity =
                identityStrategy.getValue() != null && !(identityStrategy.getValue() instanceof NoIdentityStrategy);
        if (hasPwMan && pwManIdentity.isPresent() && !hasPassword && !hasSshIdentity) {
            var perUser = pwManIdentity.get().isPerUser();
            var id = PasswordManagerIdentityStore.builder()
                    .key(inPlaceUser.getValue())
                    .perUser(perUser)
                    .build();
            showIdentityCreation(id);
            return;
        }

        var synced = DataStorage.get().getStoreEntries().stream()
                .map(entry -> entry.getStore() instanceof SyncedIdentityStore p ? p : null)
                .filter(s -> s != null)
                .findFirst();
        if (synced.isPresent()) {
            var pass = EncryptedValue.VaultKey.of(password.getValue());
            if (pass == null) {
                pass = EncryptedValue.VaultKey.of(new SecretNoneStrategy());
            }
            var ssh = EncryptedValue.VaultKey.of(identityStrategy.getValue());
            if (ssh == null) {
                ssh = EncryptedValue.VaultKey.of(new NoIdentityStrategy());
            }
            var id = SyncedIdentityStore.builder()
                    .username(inPlaceUser.getValue())
                    .password(pass)
                    .sshIdentity(ssh)
                    .build();
            showIdentityCreation(id);
            return;
        }

        var pass = EncryptedValue.CurrentKey.of(password.getValue());
        if (pass == null) {
            pass = EncryptedValue.CurrentKey.of(new SecretNoneStrategy());
        }
        var ssh = EncryptedValue.CurrentKey.of(identityStrategy.getValue());
        if (ssh == null) {
            ssh = EncryptedValue.CurrentKey.of(new NoIdentityStrategy());
        }
        var id = LocalIdentityStore.builder()
                .username(inPlaceUser.getValue())
                .password(pass)
                .sshIdentity(ssh)
                .build();
        showIdentityCreation(id);
    }

    private void showIdentityCreation(IdentityStore store) {
        StoreCreationDialog.showCreation(
                null,
                store,
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

        StoreCreationDialog.showEdit(id.get());
    }

    @Override
    public HBox createSimple() {
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
        addButton.describe(d -> d.nameKey("addReusableIdentity"));

        var nodes = new ArrayList<BaseRegionBuilder<?,?>>();
        nodes.add(createComboBox());
        nodes.add(addButton);
        var layout = new InputGroupComp(nodes).setMainReference(0).apply(struc -> struc.setFillHeight(true));

        layout.apply(struc -> {
            struc.focusedProperty().addListener((observable, oldValue, newValue) -> {
                Platform.runLater(() -> {
                    struc.getChildren().getFirst().requestFocus();
                });
            });
        });

        return layout.build();
    }

    private String formatName(DataStoreEntry storeEntry) {
        IdentityStore id = storeEntry.getStore().asNeeded();
        var suffix = id instanceof LocalIdentityStore
                ? AppI18n.get("localIdentity")
                : id instanceof PasswordManagerIdentityStore
                        ? AppI18n.get("passwordManagerIdentity")
                        : id instanceof SyncedIdentityStore && storeEntry.isPerUserStore()
                                ? AppI18n.get("userIdentity")
                                : AppI18n.get("globalIdentity");
        return storeEntry.getName() + " (" + suffix + ")";
    }

    private void applyRef(DataStoreEntryRef<IdentityStore> newRef) {
        this.selectedReference.setValue(newRef);
    }

    private BaseRegionBuilder<?,?> createComboBox() {
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
            } else {
                prop.setValue(null);
            }
        });

        var combo = new ComboTextFieldComp(
                prop, FXCollections.observableList(map.keySet().stream().toList()), () -> {
                    return new ListCell<>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                return;
                            }

                            setText(item);

                            if (item != null) {
                                var store = map.get(item);
                                if (store != null) {
                                    var provider = store.get().getProvider();
                                    var image = provider.getDisplayIconFileName(store.getStore());
                                    setGraphic(PrettyImageHelper.ofFixedSize(image, 16, 16)
                                            .build());
                                }
                            } else {
                                setGraphic(null);
                            }
                        }
                    };
                });
        combo.apply(struc -> struc.setEditable(allowUserInput));
        combo.style(Styles.LEFT_PILL);

        combo.apply(struc -> {
            var binding = Bindings.createStringBinding(
                    () -> {
                        if (selectedReference.get() != null) {
                            return selectedReference.get().get().getName();
                        }

                        return AppI18n.get("defineNewIdentityOrSelect");
                    },
                    AppI18n.activeLanguage(),
                    selectedReference);
            struc.promptTextProperty().bind(binding);
        });

        combo.apply(struc -> {
            struc.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE && !allowUserInput) {
                    selectedReference.setValue(null);
                    prop.setValue(null);
                    event.consume();
                }
            });
        });

        combo.apply(struc -> {
            var popover = new StoreChoicePopover<>(
                    null,
                    selectedReference,
                    IdentityStore.class,
                    null,
                    StoreViewState.get().getAllIdentitiesCategory(),
                    "selectIdentity",
                    "noCompatibleIdentity");

            popover.withPopover(po -> {
                ((Region) po.getContentNode()).setMaxHeight(350);
                po.showingProperty().addListener((o, oldValue, newValue) -> {
                    if (!newValue) {
                        struc.hide();
                    }
                });
            });

            var skin = new ComboBoxListViewSkin<>(struc) {
                @Override
                public void show() {
                    popover.show(struc);
                }

                @Override
                public void hide() {
                    popover.hide();
                }
            };
            MenuHelper.fixComboBoxSkin(skin);
            struc.setSkin(skin);
        });

        combo.apply(struc -> {
            struc.getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue && selectedReference.get() != null) {
                    Platform.runLater(() -> {
                        if (struc.isShowing()) {
                            return;
                        }

                        struc.getEditor().selectAll();
                    });
                }
            });
        });

        var clearButton = new IconButtonComp("mdi2c-close", () -> {
            selectedReference.setValue(null);
            inPlaceUser.setValue(null);
        });
        clearButton.style(Styles.FLAT);
        clearButton.hide(selectedReference.isNull());
        clearButton.apply(struc -> {
            struc.setOpacity(0.7);
            struc.getStyleClass().add("clear-button");
            AppFontSizes.xs(struc);
            AnchorPane.setRightAnchor(struc, 30.0);
            AnchorPane.setTopAnchor(struc, 3.0);
            AnchorPane.setBottomAnchor(struc, 3.0);
        });

        var stack = new AnchorComp(List.of(combo, clearButton));
        stack.style("identity-select-comp");
        stack.hgrow();
        stack.apply(struc -> {
            var comboRegion = (Region) struc.getChildren().getFirst();
            struc.prefWidthProperty().bind(comboRegion.prefWidthProperty());
            struc.prefHeightProperty().bind(comboRegion.prefHeightProperty());
            AnchorPane.setLeftAnchor(comboRegion, 0.0);
            AnchorPane.setRightAnchor(comboRegion, 0.0);
            struc.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    struc.getChildren().getFirst().requestFocus();
                }
            });
        });

        return stack;
    }
}
