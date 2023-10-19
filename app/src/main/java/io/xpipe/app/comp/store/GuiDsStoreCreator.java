package io.xpipe.app.comp.store;

import io.xpipe.app.comp.base.ErrorOverlayComp;
import io.xpipe.app.comp.base.MultiStepComp;
import io.xpipe.app.comp.base.PopupMenuButtonComp;
import io.xpipe.app.core.AppExtensionManager;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.ExceptionConverter;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.*;
import io.xpipe.core.store.DataStore;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class GuiDsStoreCreator extends MultiStepComp.Step<CompStructure<?>> {

    MultiStepComp parent;
    Property<DataStoreProvider> provider;
    Property<DataStore> store;
    Predicate<DataStoreProvider> filter;
    BooleanProperty busy = new SimpleBooleanProperty();
    Property<Validator> validator = new SimpleObjectProperty<>(new SimpleValidator());
    Property<String> messageProp = new SimpleStringProperty();
    BooleanProperty finished = new SimpleBooleanProperty();
    ObservableValue<DataStoreEntry> entry;
    BooleanProperty changedSinceError = new SimpleBooleanProperty();
    StringProperty name;
    DataStoreEntry existingEntry;
    boolean exists;
    boolean staticDisplay;

    public GuiDsStoreCreator(
            MultiStepComp parent,
            Property<DataStoreProvider> provider,
            Property<DataStore> store,
            Predicate<DataStoreProvider> filter,
            String initialName,
            DataStoreEntry existingEntry, boolean exists,
            boolean staticDisplay) {
        this.parent = parent;
        this.provider = provider;
        this.store = store;
        this.filter = filter;
        this.name = new SimpleStringProperty(initialName != null && !initialName.isEmpty() ? initialName : null);
        this.existingEntry = existingEntry;
        this.exists = exists;
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

        this.apply(r -> {
            r.get().setPrefWidth(AppFont.em(40));
            r.get().setPrefHeight(AppFont.em(45));
        });

        this.validator.addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                newValue.validate();
            });
        });
        this.entry = Bindings.createObjectBinding(() -> {
            if (name.getValue() == null || store.getValue() == null) {
                return null;
            }

            var testE = DataStoreEntry.createNew(
                    UUID.randomUUID(),
                    DataStorage.get().getSelectedCategory().getUuid(),
                    name.getValue(),
                    store.getValue());
            var p = provider.getValue().getDisplayParent(testE);
            return DataStoreEntry.createNew(
                    UUID.randomUUID(),
                    p != null
                            ? p.getCategoryUuid()
                            : DataStorage.get()
                            .getSelectedCategory()
                            .getUuid(),
                    name.getValue(),
                    store.getValue());
        }, name, store);
    }

    public static void showEdit(DataStoreEntry e) {
        show(
                e.getName(),
                e.getProvider(),
                e.getStore(),
                v -> true,
                newE -> {
                    ThreadHelper.runAsync(() -> {
                        if (!DataStorage.get().getStoreEntries().contains(e)) {
                            DataStorage.get().addStoreEntryIfNotPresent(newE);
                        } else {
                            DataStorage.get().updateEntry(e, newE);
                        }
                    });
                },
                true,
                true,
                e);
    }

    public static void showCreation(DataStoreProvider selected, Predicate<DataStoreProvider> filter) {
        show(
                null,
                selected,
                selected != null ? selected.defaultStore() : null,
                filter,
                e -> {
                    try {
                        DataStorage.get().addStoreEntryIfNotPresent(e);
                        if (e.getProvider().shouldHaveChildren()) {
                            ScanAlert.showAsync(e);
                        }
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).handle();
                    }
                },
                false,
                false,
                null);
    }

    private static void show(
            String initialName,
            DataStoreProvider provider,
            DataStore s,
            Predicate<DataStoreProvider> filter,
            Consumer<DataStoreEntry> con,
            boolean exists,
            boolean staticDisplay,
            DataStoreEntry existingEntry) {
        var prop = new SimpleObjectProperty<>(provider);
        var store = new SimpleObjectProperty<>(s);
        var loading = new SimpleBooleanProperty();
        var name = "addConnection";
        Platform.runLater(() -> {
            var stage = AppWindowHelper.sideWindow(
                    AppI18n.get(name),
                    window -> {
                        return new MultiStepComp() {

                            private final GuiDsStoreCreator creator = new GuiDsStoreCreator(
                                    this, prop, store, filter, initialName, existingEntry, exists, staticDisplay);

                            @Override
                            protected List<Entry> setup() {
                                loading.bind(creator.busy);
                                return List.of(new Entry(AppI18n.observable("a"), creator));
                            }

                            @Override
                            protected void finish() {
                                window.close();
                                if (creator.entry.getValue() != null) {
                                    con.accept(creator.entry.getValue());
                                }
                            }
                        };
                    },
                    false,
                    loading);
            stage.show();
        });
    }

    @Override
    public Comp<?> bottom() {
        var disable = Bindings.createBooleanBinding(
                () -> {
                    return provider.getValue() == null
                            || store.getValue() == null
                            || !store.getValue().isComplete();
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
                .disable(disable)
                .styleClass("button-comp");
    }

    private static boolean showInvalidConfirmAlert() {
        return AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("confirmInvalidStoreTitle"));
                    alert.setHeaderText(AppI18n.get("confirmInvalidStoreHeader"));
                    alert.setContentText(AppI18n.get("confirmInvalidStoreContent"));
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                })
                .map(b -> b.getButtonData().isDefaultButton())
                .orElse(false);
    }

    private Region createStoreProperties(Comp<?> comp, Validator propVal) {
        return new OptionsBuilder()
                .addComp(comp, store)
                .name("connectionName")
                .description("connectionNameDescription")
                .addString(name, false)
                .nonNull(propVal)
                .build();
    }

    @Override
    public CompStructure<? extends Region> createBase() {
        var back = Comp.of(this::createLayout);
        var message = new ErrorOverlayComp(back, messageProp);
        return message.createStructure();
    }

    private Region createLayout() {
        var layout = new BorderPane();
        layout.getStyleClass().add("store-creator");
        layout.setPadding(new Insets(20));
        var providerChoice = new DsStoreProviderChoiceComp(filter, provider, staticDisplay);
        if (staticDisplay) {
            providerChoice.apply(struc -> struc.get().setDisable(true));
        }
        providerChoice.apply(GrowAugment.create(true, false));

        SimpleChangeListener.apply(provider, n -> {
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
        layout.setTop(top);
        return layout;
    }

    @Override
    public boolean canContinue() {
        if (provider.getValue() != null) {
            var install = provider.getValue().getRequiredAdditionalInstallation();
            if (install != null && !AppExtensionManager.getInstance().isInstalled(install)) {
                ThreadHelper.runAsync(() -> {
                    try (var ignored = new BooleanScope(busy).start()) {
                        AppExtensionManager.getInstance().installIfNeeded(install);
                        /*
                        TODO: Use reload
                         */
                        finished.setValue(true);
                        OperationMode.shutdown(false, false);
                        PlatformThread.runLaterIfNeeded(parent::next);
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).handle();
                    }
                });
                return false;
            }
        }

        if (finished.get()) {
            return true;
        }

        if (store.getValue() == null) {
            return false;
        }

        if (messageProp.getValue() != null && !changedSinceError.get()) {
            if (AppPrefs.get().developerMode().getValue() && showInvalidConfirmAlert()) {
                return true;
            }
        }

        if (!exists) {
            if (name.getValue() != null
                    && DataStorage.get().getStoreEntryIfPresent(name.getValue()).isPresent()) {
                messageProp.setValue("Store with name " + name.getValue() + " does already exist");
                changedSinceError.setValue(false);
                return false;
            }
        }

        if (!validator.getValue().validate()) {
            var msg = validator
                    .getValue()
                    .getValidationResult()
                    .getMessages()
                    .get(0)
                    .getText();
            TrackEvent.info(msg);
            var newMessage = msg;
            // Temporary fix for equal error message not showing up again
            if (Objects.equals(newMessage, messageProp.getValue())) {
                newMessage = newMessage + " ";
            }
            messageProp.setValue(newMessage);
            changedSinceError.setValue(false);
            return false;
        }

        ThreadHelper.runAsync(() -> {
            try (var b = new BooleanScope(busy).start()) {
                entry.getValue().validateOrThrow();
                finished.setValue(true);
                PlatformThread.runLaterIfNeeded(parent::next);
            } catch (Exception ex) {
                var newMessage = ExceptionConverter.convertMessage(ex);
                // Temporary fix for equal error message not showing up again
                if (Objects.equals(newMessage, messageProp.getValue())) {
                    newMessage = newMessage + " ";
                }
                messageProp.setValue(newMessage);
                changedSinceError.setValue(false);
                ErrorEvent.fromThrowable(ex).omit().handle();
            }
        });
        return false;
    }
}
