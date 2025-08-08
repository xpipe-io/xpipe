package io.xpipe.ext.base.identity;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.hub.comp.*;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.EncryptedValue;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.SecretRetrievalStrategy;

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
        var pass = EncryptedValue.CurrentKey.of(password.getValue());
        if (pass == null) {
            pass = EncryptedValue.CurrentKey.of(new SecretRetrievalStrategy.None());
        }
        var ssh = EncryptedValue.CurrentKey.of(identityStrategy.getValue());
        if (ssh == null) {
            ssh = EncryptedValue.CurrentKey.of(new SshIdentityStrategy.None());
        }
        var id = LocalIdentityStore.builder()
                .username(inPlaceUser.getValue())
                .password(pass)
                .sshIdentity(ssh)
                .build();

        StoreCreationDialog.showCreation(
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

        StoreCreationDialog.showEdit(id.get());
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
                Platform.runLater(() -> {
                    struc.get().getChildren().getFirst().requestFocus();
                });
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
            } else {
                prop.setValue(null);
            }
        });

        var combo = new ComboTextFieldComp(prop, FXCollections.observableList(map.keySet().stream().toList()), () -> {
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
                            setGraphic(
                                    PrettyImageHelper.ofFixedSize(image, 16, 16).createRegion());
                        }
                    } else {
                        setGraphic(null);
                    }
                }
            };
        });
        combo.apply(struc -> struc.get().setEditable(allowUserInput));
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
                    AppI18n.activeLanguage(),
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

        combo.apply(struc -> {
            var popover = new StoreChoicePopover<>(null, selectedReference, IdentityStore.class, null,
                    StoreViewState.get().getAllIdentitiesCategory(), "selectIdentity");
            ((Region) popover.getPopover().getContentNode()).setMaxHeight(350);
            var skin = new ComboBoxListViewSkin<>(struc.get()) {
                @Override
                public void show() {
                    popover.show(struc.get());
                }

                @Override
                public void hide() {
                    popover.hide();
                }
            };
            popover.getPopover().showingProperty().addListener((o, oldValue, newValue) -> {
                if (!newValue) {
                    struc.get().hide();
                }
            });
            struc.get().setSkin(skin);
        });

        combo.apply(struc -> {
            struc.get().getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue && selectedReference.get() != null) {
                    Platform.runLater(() -> {
                        if (struc.get().isShowing()) {
                            return;
                        }

                        struc.get().getEditor().selectAll();
                    });
                }
            });
        });

        var clearButton = new IconButtonComp("mdi2c-close", () -> {
            selectedReference.setValue(null);
            inPlaceUser.setValue(null);
        });
        clearButton.styleClass(Styles.FLAT);
        clearButton.hide(selectedReference.isNull());
        clearButton.apply(struc -> {
            struc.get().setOpacity(0.7);
            struc.get().getStyleClass().add("clear-button");
            AppFontSizes.xs(struc.get());
            AnchorPane.setRightAnchor(struc.get(), 30.0);
            AnchorPane.setTopAnchor(struc.get(), 3.0);
            AnchorPane.setBottomAnchor(struc.get(), 3.0);
        });

        var stack = new AnchorComp(List.of(combo, clearButton));
        stack.styleClass("identity-select-comp");
        stack.hgrow();
        stack.apply(struc -> {
            var comboRegion = (Region) struc.get().getChildren().getFirst();
            struc.get().prefWidthProperty().bind(comboRegion.prefWidthProperty());
            struc.get().prefHeightProperty().bind(comboRegion.prefHeightProperty());
            AnchorPane.setLeftAnchor(comboRegion, 0.0);
            AnchorPane.setRightAnchor(comboRegion, 0.0);
            struc.get().focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    struc.get().getChildren().getFirst().requestFocus();
                }
            });
        });

        return stack;
    }
}
