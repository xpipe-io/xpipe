package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.augment.GrowAugment;
import io.xpipe.app.comp.base.*;
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
import lombok.experimental.FieldDefaults;
import net.synedra.validatorfx.GraphicDecorationStackPane;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class StoreCreationComp extends DialogComp {

    Stage window;
    CreationConsumer consumer;
    Property<DataStoreProvider> provider;
    ObjectProperty<DataStore> store;
    Predicate<DataStoreProvider> filter;
    BooleanProperty busy = new SimpleBooleanProperty();
    Property<Validator> validator = new SimpleObjectProperty<>(new SimpleValidator());
    Property<ModalOverlay> messageProp = new SimpleObjectProperty<>();
    BooleanProperty finished = new SimpleBooleanProperty();
    ObservableValue<DataStoreEntry> entry;
    BooleanProperty changedSinceError = new SimpleBooleanProperty();
    BooleanProperty skippable = new SimpleBooleanProperty();
    BooleanProperty connectable = new SimpleBooleanProperty();
    StringProperty name;
    DataStoreEntry existingEntry;
    boolean staticDisplay;

    public StoreCreationComp(
            Stage window,
            CreationConsumer consumer,
            Property<DataStoreProvider> provider,
            ObjectProperty<DataStore> store,
            Predicate<DataStoreProvider> filter,
            String initialName,
            DataStoreEntry existingEntry,
            boolean staticDisplay) {
        this.window = window;
        this.consumer = consumer;
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

        this.apply(r -> {
            r.get().setPrefWidth(650);
            r.get().setPrefHeight(750);
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

    public static void showEdit(DataStoreEntry e) {
        show(
                e.getName(),
                e.getProvider(),
                e.getStore(),
                v -> true,
                (newE, validated) -> {
                    ThreadHelper.runAsync(() -> {
                        if (!DataStorage.get().getStoreEntries().contains(e)) {
                            DataStorage.get().addStoreEntryIfNotPresent(newE);
                        } else {
                            // We didn't change anything
                            if (e.getStore().equals(newE.getStore())) {
                                e.setName(newE.getName());
                            } else {
                                DataStorage.get().updateEntry(e, newE);
                            }
                        }
                    });
                },
                true,
                e);
    }

    public static void showCreation(DataStoreProvider selected, DataStoreCreationCategory category) {
        showCreation(selected != null ? selected.defaultStore() : null, category, dataStoreEntry -> {}, true);
    }

    public static void showCreation(
            DataStore base,
            DataStoreCreationCategory category,
            Consumer<DataStoreEntry> listener,
            boolean selectCategory) {
        var prov = base != null ? DataStoreProviders.byStore(base) : null;
        show(
                null,
                prov,
                base,
                dataStoreProvider -> (category != null && category.equals(dataStoreProvider.getCreationCategory()))
                        || dataStoreProvider.equals(prov),
                (e, validated) -> {
                    try {
                        var returned = DataStorage.get().addStoreEntryIfNotPresent(e);
                        listener.accept(returned);
                        if (validated
                                && e.getProvider().shouldShowScan()
                                && AppPrefs.get()
                                        .openConnectionSearchWindowOnConnectionCreation()
                                        .get()) {
                            ScanDialog.showAsync(e);
                        }

                        if (selectCategory) {
                            // Select new category if needed
                            var cat = DataStorage.get()
                                    .getStoreCategoryIfPresent(e.getCategoryUuid())
                                    .orElseThrow();
                            PlatformThread.runLaterIfNeeded(() -> {
                                StoreViewState.get()
                                        .getActiveCategory()
                                        .setValue(StoreViewState.get().getCategoryWrapper(cat));
                            });
                        }
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).handle();
                    }
                },
                false,
                null);
    }

    public interface CreationConsumer {

        void consume(DataStoreEntry entry, boolean validated);
    }

    private static void show(
            String initialName,
            DataStoreProvider provider,
            DataStore s,
            Predicate<DataStoreProvider> filter,
            CreationConsumer con,
            boolean staticDisplay,
            DataStoreEntry existingEntry) {
        var prop = new SimpleObjectProperty<>(provider);
        var store = new SimpleObjectProperty<>(s);
        DialogComp.showWindow(
                "addConnection",
                stage -> new StoreCreationComp(
                        stage, con, prop, store, filter, initialName, existingEntry, staticDisplay));
    }

    private static boolean showInvalidConfirmAlert() {
        return AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("confirmInvalidStoreTitle"));
                    alert.setHeaderText(AppI18n.get("confirmInvalidStoreHeader"));
                    alert.getDialogPane()
                            .setContent(AppWindowHelper.alertContentText(AppI18n.get("confirmInvalidStoreContent")));
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                    alert.getButtonTypes().clear();
                    alert.getButtonTypes().add(new ButtonType(AppI18n.get("retry"), ButtonBar.ButtonData.CANCEL_CLOSE));
                    alert.getButtonTypes().add(new ButtonType(AppI18n.get("skip"), ButtonBar.ButtonData.OK_DONE));
                })
                .map(b -> b.getButtonData().isDefaultButton())
                .orElse(false);
    }

    @Override
    protected List<Comp<?>> customButtons() {
        return List.of(
                new ButtonComp(AppI18n.observable("skipValidation"), () -> {
                            if (showInvalidConfirmAlert()) {
                                commit(false);
                            } else {
                                finish();
                            }
                        })
                        .visible(skippable),
                new ButtonComp(AppI18n.observable("connect"), () -> {
                            var temp = DataStoreEntry.createTempWrapper(store.getValue());
                            var action = provider.getValue().launchAction(temp);
                            ThreadHelper.runFailableAsync(() -> {
                                action.execute();
                            });
                        })
                        .hide(connectable
                                .not()
                                .or(Bindings.createBooleanBinding(
                                        () -> {
                                            return store.getValue() == null
                                                    || !store.getValue().isComplete();
                                        },
                                        store))));
    }

    @Override
    protected ObservableValue<Boolean> busy() {
        return busy;
    }

    @Override
    protected void discard() {}

    @Override
    protected void finish() {
        if (finished.get()) {
            return;
        }

        if (store.getValue() == null) {
            return;
        }

        // We didn't change anything
        if (existingEntry != null && existingEntry.getStore().equals(store.getValue())) {
            commit(false);
            return;
        }

        if (!validator.getValue().validate()) {
            var msg = validator
                    .getValue()
                    .getValidationResult()
                    .getMessages()
                    .getFirst()
                    .getText();
            TrackEvent.info(msg);
            messageProp.setValue(createErrorOverlay(msg));
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
                commit(true);
            } catch (Throwable ex) {
                String message;
                if (ex instanceof ValidationException) {
                    ErrorEvent.expected(ex);
                    message = ex.getMessage();
                } else if (ex instanceof StackOverflowError) {
                    // Cycles in connection graphs can fail hard but are expected
                    ErrorEvent.expected(ex);
                    message = "StackOverflowError";
                } else {
                    message = ex.getMessage();
                }

                messageProp.setValue(createErrorOverlay(message));
                changedSinceError.setValue(false);

                ErrorEvent.fromThrowable(ex).omit().handle();
            } finally {
                DataStorage.get().removeStoreEntryInProgress(entry.getValue());
            }
        });
    }

    @Override
    public Comp<?> content() {
        return Comp.of(this::createLayout);
    }

    @Override
    protected Comp<?> pane(Comp<?> content) {
        var back = super.pane(content);
        return new ModalOverlayComp(back, messageProp);
    }

    private ModalOverlay createErrorOverlay(String message) {
        var comp = Comp.of(() -> {
            var l = new TextArea();
            l.setText(message);
            l.setWrapText(true);
            l.getStyleClass().add("error-overlay-comp");
            l.setEditable(false);
            return l;
        });
        var overlay = ModalOverlay.of("error", comp, new LabelGraphic.NodeGraphic(() -> {
            var graphic = new FontIcon("mdomz-warning");
            graphic.setIconColor(Color.RED);
            return new StackPane(graphic);
        }));
        return overlay;
    }

    @Override
    public Comp<?> bottom() {
        var disable = Bindings.createBooleanBinding(
                () -> {
                    return provider.getValue() == null
                            || store.getValue() == null
                            || !store.getValue().isComplete()
                            // When switching providers, both observables change one after another.
                            // So temporarily there might be a store class mismatch
                            || provider.getValue().getStoreClasses().stream()
                                    .noneMatch(aClass -> aClass.isAssignableFrom(
                                            store.getValue().getClass()))
                            || provider.getValue().createInsightsMarkdown(store.getValue()) == null;
                },
                provider,
                store);
        return new PopupMenuButtonComp(
                        new SimpleStringProperty("Insights >"),
                        Comp.of(() -> {
                            return provider.getValue() != null
                                    ? provider.getValue()
                                            .createInsightsComp(store)
                                            .createRegion()
                                    : null;
                        }),
                        true)
                .hide(disable)
                .styleClass("button-comp");
    }

    private Region createStoreProperties(Comp<?> comp, Validator propVal) {
        var p = provider.getValue();
        var nameKey = p == null
                        || p.getCreationCategory() == null
                        || p.getCreationCategory().getCategory().equals(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID)
                ? "connection"
                : p.getCreationCategory().getCategory().equals(DataStorage.ALL_SCRIPTS_CATEGORY_UUID)
                        ? "script"
                        : "identity";
        return new OptionsBuilder()
                .addComp(comp, store)
                .name(nameKey + "Name")
                .description(nameKey + "NameDescription")
                .addString(name, false)
                .nonNull(propVal)
                .buildComp()
                .onSceneAssign(struc -> {
                    if (staticDisplay) {
                        struc.get().requestFocus();
                    }
                })
                .createRegion();
    }

    private void commit(boolean validated) {
        if (finished.get()) {
            return;
        }
        finished.setValue(true);

        if (entry.getValue() != null) {
            consumer.consume(entry.getValue(), validated);
        }

        PlatformThread.runLaterIfNeeded(() -> {
            window.close();
        });
    }

    private Region createLayout() {
        var layout = new BorderPane();
        layout.getStyleClass().add("store-creator");
        var providerChoice = new StoreProviderChoiceComp(filter, provider);
        var showProviders = (!staticDisplay
                        && (providerChoice.getProviders().size() > 1
                                || providerChoice.getProviders().getFirst().showProviderChoice()))
                || (staticDisplay && provider.getValue().showProviderChoice());
        if (staticDisplay) {
            providerChoice.apply(struc -> struc.get().setDisable(true));
        }
        if (showProviders) {
            providerChoice.onSceneAssign(struc -> struc.get().requestFocus());
        }
        providerChoice.apply(GrowAugment.create(true, false));

        provider.subscribe(n -> {
            if (n != null) {
                var d = n.guiDialog(existingEntry, store);
                var propVal = new SimpleValidator();
                var propR = createStoreProperties(d == null || d.getComp() == null ? null : d.getComp(), propVal);

                var sp = new ScrollPane(propR);
                sp.setFitToWidth(true);
                layout.setCenter(sp);

                validator.setValue(new ChainedValidator(List.of(
                        d != null && d.getValidator() != null ? d.getValidator() : new SimpleValidator(), propVal)));
            } else {
                layout.setCenter(null);
                validator.setValue(new SimpleValidator());
            }
        });

        var sep = new Separator();
        sep.getStyleClass().add("spacer");
        var top = new VBox(providerChoice.createRegion(), sep);
        top.getStyleClass().add("top");
        if (showProviders) {
            layout.setTop(top);
            layout.setPadding(new Insets(15, 20, 20, 20));
        } else {
            layout.setPadding(new Insets(5, 20, 20, 20));
        }

        var valSp = new GraphicDecorationStackPane();
        valSp.getChildren().add(layout);
        return valSp;
    }
}
