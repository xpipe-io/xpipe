package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.augment.GrowAugment;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.DialogComp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.comp.base.ModalOverlayComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.*;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ValidatableStore;
import io.xpipe.core.util.ValidationException;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.synedra.validatorfx.GraphicDecorationStackPane;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
public class StoreCreationModel {

    Property<DataStoreProvider> provider;
    ObjectProperty<DataStore> store;
    Predicate<DataStoreProvider> filter;
    BooleanProperty busy = new SimpleBooleanProperty();
    Property<Validator> validator = new SimpleObjectProperty<>(new SimpleValidator());
    BooleanProperty finished = new SimpleBooleanProperty();
    ObservableValue<DataStoreEntry> entry;
    BooleanProperty changedSinceError = new SimpleBooleanProperty();
    BooleanProperty skippable = new SimpleBooleanProperty();
    BooleanProperty connectable = new SimpleBooleanProperty();
    StringProperty name;
    DataStoreEntry existingEntry;
    boolean staticDisplay;

    public StoreCreationModel(
            Property<DataStoreProvider> provider,
            ObjectProperty<DataStore> store, Predicate<DataStoreProvider> filter,
            String initialName,
            DataStoreEntry existingEntry,
            boolean staticDisplay) {
        this.provider = provider;
        this.store = store;
        this.filter = filter;
        this.name = new SimpleStringProperty(initialName != null && !initialName.isEmpty() ? initialName : null);
        this.existingEntry = existingEntry;
        this.staticDisplay = staticDisplay;
        this.store.addListener((c, o, n) -> {
            changedSinceError.setValue(true);
        });
        this.name.addListener((c, o, n) -> {
            changedSinceError.setValue(true);
        });

        this.provider.addListener((c, o, n) -> {
            store.unbind();
            store.setValue(null);
            if (n != null) {
                store.setValue(n.defaultStore());
            }
        });

        this.provider.subscribe((n) -> {
            if (n != null) {
                connectable.setValue(n.canConnectDuringCreation());
            }
        });

        this.validator.addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                newValue.validate();
            });
        });
        this.entry = Bindings.createObjectBinding(
                () -> {
                    if (name.getValue() == null || store.getValue() == null) {
                        return null;
                    }

                    var testE = DataStoreEntry.createNew(
                            UUID.randomUUID(),
                            DataStorage.get().getSelectedCategory().getUuid(),
                            name.getValue(),
                            store.getValue());
                    var p = DataStorage.get().getDefaultDisplayParent(testE).orElse(null);

                    var targetCategory = p != null
                            ? p.getCategoryUuid()
                            : DataStorage.get().getSelectedCategory().getUuid();
                    var rootCategory = DataStorage.get()
                            .getRootCategory(DataStorage.get()
                                    .getStoreCategoryIfPresent(targetCategory)
                                    .orElseThrow());

                    // Don't put it in the wrong root category
                    if ((provider.getValue().getCreationCategory() == null
                            || !provider.getValue()
                                    .getCreationCategory()
                                    .getCategory()
                                    .equals(rootCategory.getUuid()))) {
                        targetCategory = provider.getValue().getCreationCategory() != null
                                ? provider.getValue().getCreationCategory().getCategory()
                                : DataStorage.ALL_CONNECTIONS_CATEGORY_UUID;
                    }

                    // Don't use the all connections category
                    if (targetCategory.equals(
                            DataStorage.get().getAllConnectionsCategory().getUuid())) {
                        targetCategory = DataStorage.get()
                                .getDefaultConnectionsCategory()
                                .getUuid();
                    }

                    // Don't use the all scripts category
                    if (targetCategory.equals(
                            DataStorage.get().getAllScriptsCategory().getUuid())) {
                        targetCategory = DataStorage.CUSTOM_SCRIPTS_CATEGORY_UUID;
                    }

                    // Don't use the all identities category
                    if (targetCategory.equals(
                            DataStorage.get().getAllIdentitiesCategory().getUuid())) {
                        targetCategory = DataStorage.LOCAL_IDENTITIES_CATEGORY_UUID;
                    }

                    // Custom category stuff
                    targetCategory = provider.getValue().getTargetCategory(store.getValue(), targetCategory);

                    return DataStoreEntry.createNew(
                            UUID.randomUUID(), targetCategory, name.getValue(), store.getValue());
                },
                name,
                store);

        skippable.bind(Bindings.createBooleanBinding(
                () -> {
                    if (name.get() != null && store.get().isComplete() && store.get() instanceof ValidatableStore) {
                        return true;
                    } else {
                        return false;
                    }
                },
                store,
                name));
    }

    ObservableBooleanValue canConnect() {
        return connectable
                .not()
                .or(Bindings.createBooleanBinding(
                        () -> {
                            return store.getValue() == null
                                    || !store.getValue().isComplete();
                        },
                        store));
    }

    void connect() {
        var temp = DataStoreEntry.createTempWrapper(store.getValue());
        var action = provider.getValue().launchAction(temp);
        ThreadHelper.runFailableAsync(() -> {
            action.execute();
        });
    }

    ObservableValue<Boolean> busy() {
        return busy;
    }

    void finish() {
        if (finished.get()) {
            return;
        }

        if (store.getValue() == null) {
            return;
        }

        // We didn't change anything
        if (existingEntry != null && existingEntry.getStore().equals(store.getValue())) {
            commit();
            return;
        }

        if (!validator.getValue().validate()) {
            var msg = validator
                    .getValue()
                    .getValidationResult()
                    .getMessages()
                    .getFirst()
                    .getText();
            ErrorEvent.fromMessage(msg).handle();
            changedSinceError.setValue(false);
            return;
        }

        ThreadHelper.runAsync(() -> {
            // Might have changed since last time
            if (entry.getValue() == null) {
                return;
            }

            try (var ignored = new BooleanScope(busy).start()) {
                DataStorage.get().addStoreEntryInProgress(entry.getValue());
                entry.getValue().validateOrThrow();
                commit();
            } catch (Throwable ex) {
                if (ex instanceof ValidationException) {
                    ErrorEvent.expected(ex);
                } else if (ex instanceof StackOverflowError) {
                    // Cycles in connection graphs can fail hard but are expected
                    ErrorEvent.expected(ex);
                }

                changedSinceError.setValue(false);

                ErrorEvent.fromThrowable(ex).handle();
            } finally {
                DataStorage.get().removeStoreEntryInProgress(entry.getValue());
            }
        });
    }

    void showDocs() {
        Hyperlinks.open(provider.getValue().getHelpLink());
    }

    ObservableBooleanValue canShowDocs() {
        var disable = Bindings.createBooleanBinding(
                () -> {
                    return provider.getValue() == null || provider.getValue().getHelpLink() == null;
                },
                provider);
        return disable;
    }

    void commit() {
        if (finished.get()) {
            return;
        }
        finished.setValue(true);
    }

    public String storeTypeNameKey() {
        var p = provider.getValue();
        var nameKey = p == null
                || p.getCreationCategory() == null
                || p.getCreationCategory().getCategory().equals(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID)
                ? "connection"
                : p.getCreationCategory().getCategory().equals(DataStorage.ALL_SCRIPTS_CATEGORY_UUID)
                ? "script"
                : "identity";
        return nameKey;
    }
}
