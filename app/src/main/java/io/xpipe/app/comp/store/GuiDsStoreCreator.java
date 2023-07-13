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
    Property<DataStoreEntry> entry = new SimpleObjectProperty<>();
    BooleanProperty changedSinceError = new SimpleBooleanProperty();
    StringProperty name;
    boolean exists;

    public GuiDsStoreCreator(
            MultiStepComp parent,
            Property<DataStoreProvider> provider,
            Property<DataStore> store,
            Predicate<DataStoreProvider> filter,
            String initialName,
            boolean exists) {
        this.parent = parent;
        this.provider = provider;
        this.store = store;
        this.filter = filter;
        this.name = new SimpleStringProperty(initialName != null && !initialName.isEmpty() ? initialName : null);
        this.exists = exists;
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
    }

    public static void showEdit(DataStoreEntry e) {
        show(
                e.getName(),
                e.getProvider(),
                e.getStore(),
                v -> true,
                newE -> {
                    ThreadHelper.runAsync(() -> {
                        e.applyChanges(newE);
                        if (!DataStorage.get().getStoreEntries().contains(e)) {
                            DataStorage.get().addStoreEntry(e);
                        }
                        DataStorage.get().refresh();
                    });
                },
                true);
    }

    public static void showCreation(Predicate<DataStoreProvider> filter) {
        show(
                null,
                null,
                null,
                filter,
                e -> {
                    try {
                        DataStorage.get().addStoreEntry(e);
                        if (e.getProvider().shouldHaveChildren()) {
                            ScanAlert.showAsync(e);
                        }
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).handle();
                    }
                },
                false);
    }

    public static void show(
            String initialName,
            DataStoreProvider provider,
            DataStore s,
            Predicate<DataStoreProvider> filter,
            Consumer<DataStoreEntry> con,
            boolean exists) {
        var prop = new SimpleObjectProperty<>(provider);
        var store = new SimpleObjectProperty<>(s);
        var loading = new SimpleBooleanProperty();
        var name = "addConnection";
        Platform.runLater(() -> {
            var stage = AppWindowHelper.sideWindow(
                    AppI18n.get(name),
                    window -> {
                        return new MultiStepComp() {

                            private final GuiDsStoreCreator creator =
                                    new GuiDsStoreCreator(this, prop, store, filter, initialName, exists);

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
                .bind(
                        () -> {
                            if (name.getValue() == null || store.getValue() == null) {
                                return null;
                            }

                            return DataStoreEntry.createNew(UUID.randomUUID(), name.getValue(), store.getValue());
                        },
                        entry)
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
        var providerChoice = new DsStoreProviderChoiceComp(filter, provider);
        if (provider.getValue() != null) {
            providerChoice.apply(struc -> struc.get().setDisable(true));
        }
        providerChoice.apply(GrowAugment.create(true, false));

        SimpleChangeListener.apply(provider, n -> {
            if (n != null) {
                //                var install = n.getRequiredAdditionalInstallation();
                //                if (install != null && AppExtensionManager.getInstance().isInstalled(install)) {
                //                    layout.setCenter(new InstallExtensionComp((DownloadModuleInstall)
                // install).createRegion());
                //                    validator.setValue(new SimpleValidator());
                //                    return;
                //                }

                var d = n.guiDialog(store);
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
                    try (var ignored = new BusyProperty(busy)) {
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
            try (var b = new BusyProperty(busy)) {
                entry.getValue().refresh(true);
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
