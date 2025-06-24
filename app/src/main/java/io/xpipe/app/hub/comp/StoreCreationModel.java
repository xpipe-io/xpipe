package io.xpipe.app.hub.comp;

import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.action.impl.LaunchHubMenuLeafProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.*;
import io.xpipe.core.process.ShellTtyState;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ValidatableStore;
import io.xpipe.core.util.ValidationException;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;
import java.util.function.Predicate;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
public class StoreCreationModel {

    ObjectProperty<DataStore> initialStore = new SimpleObjectProperty<>();
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
    StoreCreationConsumer consumer;

    public StoreCreationModel(
            Property<DataStoreProvider> provider,
            ObjectProperty<DataStore> store,
            Predicate<DataStoreProvider> filter,
            String initialName,
            DataStoreEntry existingEntry,
            boolean staticDisplay,
            StoreCreationConsumer consumer) {
        this.provider = provider;
        this.store = store;
        this.filter = filter;
        this.name = new SimpleStringProperty(initialName != null && !initialName.isEmpty() ? initialName : null);
        this.existingEntry = existingEntry;
        this.staticDisplay = staticDisplay;
        this.consumer = consumer;
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
                store.setValue(n.defaultStore(getTargetCategory(existingEntry)));
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
                    if (name.getValue() == null
                            || store.getValue() == null
                            || name.getValue().isBlank()) {
                        return null;
                    }

                    var testE = DataStoreEntry.createNew(
                            UUID.randomUUID(),
                            DataStorage.get().getSelectedCategory().getUuid(),
                            name.getValue(),
                            store.getValue());
                    var p = DataStorage.get().getDefaultDisplayParent(testE).orElse(null);
                    var targetCategory = getTargetCategory(p);
                    return DataStoreEntry.createNew(
                            UUID.randomUUID(), targetCategory.getUuid(), name.getValue(), store.getValue());
                },
                name,
                store);

        skippable.bind(Bindings.createBooleanBinding(
                () -> {
                    if (entry.getValue() != null
                            && store.get().isComplete()
                            && store.get() instanceof ValidatableStore) {
                        return true;
                    } else {
                        return false;
                    }
                },
                store,
                name));
    }

    private DataStoreCategory getTargetCategory(DataStoreEntry base) {
        var targetCategory = base != null
                ? base.getCategoryUuid()
                : DataStorage.get().getSelectedCategory().getUuid();
        var rootCategory = DataStorage.get()
                .getRootCategory(DataStorage.get()
                        .getStoreCategoryIfPresent(targetCategory)
                        .orElse(DataStorage.get().getAllConnectionsCategory()));

        // Don't put it in the wrong root category
        if ((provider.getValue().getCreationCategory() == null
                || !provider.getValue().getCreationCategory().getCategory().equals(rootCategory.getUuid()))) {
            targetCategory = provider.getValue().getCreationCategory() != null
                    ? provider.getValue().getCreationCategory().getCategory()
                    : DataStorage.ALL_CONNECTIONS_CATEGORY_UUID;
        }

        // Don't use the all connections category
        if (targetCategory.equals(DataStorage.get().getAllConnectionsCategory().getUuid())) {
            targetCategory = DataStorage.get().getDefaultConnectionsCategory().getUuid();
        }

        // Don't use the all scripts category
        if (targetCategory.equals(DataStorage.get().getAllScriptsCategory().getUuid())) {
            targetCategory = DataStorage.CUSTOM_SCRIPTS_CATEGORY_UUID;
        }

        // Don't use the all identities category
        if (targetCategory.equals(DataStorage.get().getAllIdentitiesCategory().getUuid())) {
            targetCategory = DataStorage.LOCAL_IDENTITIES_CATEGORY_UUID;
        }

        // Custom category stuff
        targetCategory = provider.getValue().getTargetCategory(store.getValue(), targetCategory);
        return DataStorage.get().getStoreCategoryIfPresent(targetCategory).orElseThrow();
    }

    ObservableBooleanValue canConnect() {
        return connectable
                .not()
                .or(Bindings.createBooleanBinding(
                        () -> {
                            return store.getValue() == null || !store.getValue().isComplete();
                        },
                        store));
    }

    void connect() {
        var temp = entry.getValue() != null ? entry.getValue() : DataStoreEntry.createTempWrapper(store.getValue());
        var action = LaunchHubMenuLeafProvider.Action.builder().ref(temp.ref()).build();
        action.executeAsync();
    }

    boolean hasBeenModified() {
        if (initialStore.getValue() == null) {
            return true;
        }

        var eq = initialStore.getValue().equals(store.getValue());
        return !eq;
    }

    void finish() {
        if (finished.get()) {
            return;
        }

        if (store.getValue() == null) {
            return;
        }

        if (!validator.getValue().validate()) {
            var msg = validator
                    .getValue()
                    .getValidationResult()
                    .getMessages()
                    .getFirst()
                    .getText();
            ErrorEventFactory.fromMessage(msg).expected().handle();
            changedSinceError.setValue(false);
            return;
        }

        // We didn't change anything
        if (existingEntry != null && existingEntry.getStore().equals(store.getValue())) {
            commit(false);
            return;
        }

        if (entry.getValue() == null) {
            return;
        }

        ThreadHelper.runAsync(() -> {
            // Might have changed since last time
            if (entry.getValue() == null) {
                return;
            }

            try (var ignored = new BooleanScope(busy).start()) {
                DataStorage.get().addStoreEntryInProgress(entry.getValue());
                validate();
                commit(true);
            } catch (Throwable ex) {
                if (ex instanceof ValidationException) {
                    ErrorEventFactory.expected(ex);
                } else if (ex instanceof StackOverflowError) {
                    // Cycles in connection graphs can fail hard but are expected
                    ErrorEventFactory.expected(ex);
                }

                changedSinceError.setValue(false);

                ErrorEventFactory.fromThrowable(ex).handle();
            } finally {
                DataStorage.get().removeStoreEntryInProgress(entry.getValue());
            }
        });
    }

    private void validate() throws Throwable {
        var s = entry.getValue().getStore();
        if (s == null) {
            return;
        }

        s.checkComplete();

        // Start session for later
        if (s instanceof ShellStore ss) {
            var sc = ss.getOrStartSession();
            var unsupported = !sc.getShellDialect().getDumbMode().supportsAnyPossibleInteraction()
                    || sc.getTtyState() != ShellTtyState.NONE;
            if (unsupported) {
                ss.stopSessionIfNeeded();
            }
        } else if (s instanceof ValidatableStore vs) {
            vs.validate();
        }
    }

    void showDocs() {
        Hyperlinks.open(provider.getValue().getHelpLink().getLink());
    }

    ObservableBooleanValue canShowDocs() {
        var disable = Bindings.createBooleanBinding(
                () -> {
                    return provider.getValue() == null || provider.getValue().getHelpLink() == null;
                },
                provider);
        return disable;
    }

    void commit(boolean validated) {
        if (finished.get()) {
            return;
        }

        finished.setValue(true);
        consumer.consume(entry.getValue(), validated);
    }

    public String storeTypeNameKey() {
        var p = provider.getValue();
        var nameKey = p == null
                        || p.getCreationCategory() == null
                        || p.getCreationCategory().getCategory().equals(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID)
                ? "connection"
                : p.getCreationCategory().getCategory().equals(DataStorage.ALL_SCRIPTS_CATEGORY_UUID)
                        ? (p.getId().equals("scriptGroup") ? "scriptGroup" : "script")
                        : p.getCreationCategory().getCategory().equals(DataStorage.ALL_IDENTITIES_CATEGORY_UUID)
                                ? "identity"
                                : "macro";
        return nameKey;
    }
}
