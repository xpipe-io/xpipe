package io.xpipe.app.comp.source.store;

import io.xpipe.app.comp.base.InstallExtensionComp;
import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.comp.base.MessageComp;
import io.xpipe.app.comp.base.MultiStepComp;
import io.xpipe.app.core.AppExtensionManager;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DownloadModuleInstall;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.ExceptionConverter;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.*;
import io.xpipe.core.store.DataStore;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class GuiDsStoreCreator extends MultiStepComp.Step<CompStructure<?>> {

    MultiStepComp parent;
    Property<DataStoreProvider> provider;
    Property<DataStore> input;
    Predicate<DataStoreProvider> filter;
    BooleanProperty busy = new SimpleBooleanProperty();
    Property<Validator> validator = new SimpleObjectProperty<>(new SimpleValidator());
    Property<String> messageProp = new SimpleStringProperty();
    MessageComp message = new MessageComp(messageProp, 10000);
    BooleanProperty finished = new SimpleBooleanProperty();
    Property<DataStoreEntry> entry = new SimpleObjectProperty<>();
    BooleanProperty changedSinceError = new SimpleBooleanProperty();
    StringProperty name;

    public GuiDsStoreCreator(
            MultiStepComp parent,
            Property<DataStoreProvider> provider,
            Property<DataStore> input,
            Predicate<DataStoreProvider> filter,
            String initialName) {
        super(null);
        this.parent = parent;
        this.provider = provider;
        this.input = input;
        this.filter = filter;
        this.name = new SimpleStringProperty(initialName);
        this.input.addListener((c, o, n) -> {
            changedSinceError.setValue(true);
        });
        this.name.addListener((c, o, n) -> {
            changedSinceError.setValue(true);
        });

        this.provider.addListener((c, o, n) -> {
            input.unbind();
            input.setValue(null);
            if (n != null) {
                input.setValue(n.defaultStore());
            }
        });

        this.apply(r -> {
            r.get().setPrefWidth(AppFont.em(36));
            r.get().setPrefHeight(AppFont.em(42));
        });

        this.validator.addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                newValue.validate();
            });
        });
    }

    public static void showEdit(DataStoreEntry e) {
        show(e.getName(), e.getProvider(), e.getStore(), v -> true, newE -> {
            ThreadHelper.runAsync(() -> {
                e.applyChanges(newE);
                if (!DataStorage.get().getStoreEntries().contains(e)) {
                    DataStorage.get().addStoreEntry(e);
                    ScanAlert.showIfNeeded(e.getStore(), true);
                }
                DataStorage.get().refresh();
            });
        });
    }

    public static void showCreation(Predicate<DataStoreProvider> filter) {
        show(null, null, null, filter, e -> {
            try {
                DataStorage.get().addStoreEntry(e);
                ScanAlert.showIfNeeded(e.getStore(), true);
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).handle();
            }
        });
    }

    public static void show(
            String initialName,
            DataStoreProvider provider,
            DataStore s,
            Predicate<DataStoreProvider> filter,
            Consumer<DataStoreEntry> con) {
        var prop = new SimpleObjectProperty<DataStoreProvider>(provider);
        var store = new SimpleObjectProperty<DataStore>(s);
        var name = "addConnection";
        Platform.runLater(() -> {
            var stage = AppWindowHelper.sideWindow(
                    AppI18n.get(name),
                    window -> {
                        return new MultiStepComp() {

                            private final GuiDsStoreCreator creator =
                                    new GuiDsStoreCreator(this, prop, store, filter, initialName);

                            @Override
                            protected List<Entry> setup() {
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
                    null);
            stage.show();
        });
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
                .addComp(comp, input)
                .name("connectionName")
                .description("connectionNameDescription")
                .addString(name, false)
                .nonNull(propVal)
                .bind(
                        () -> {
                            if (name.getValue() == null || input.getValue() == null) {
                                return null;
                            }

                            return DataStoreEntry.createNew(UUID.randomUUID(), name.getValue(), input.getValue());
                        },
                        entry)
                .build();
    }

    @Override
    public CompStructure<? extends Region> createBase() {
        var layout = new BorderPane();
        var providerChoice = new DsStoreProviderChoiceComp(filter, provider);
        if (provider.getValue() != null) {
            providerChoice.apply(struc -> struc.get().setDisable(true));
        }
        providerChoice.apply(GrowAugment.create(true, false));

        SimpleChangeListener.apply(provider, n -> {
            if (n != null) {
                var install = n.getRequiredAdditionalInstallation();
                if (install != null && AppExtensionManager.getInstance().isInstalled(install)) {
                    layout.setCenter(new InstallExtensionComp((DownloadModuleInstall) install).createRegion());
                    validator.setValue(new SimpleValidator());
                    return;
                }

                var d = n.guiDialog(input);
                var propVal = new SimpleValidator();
                var propR = createStoreProperties(d == null || d.getComp() == null ? null : d.getComp(), propVal);
                var box = new VBox(propR);
                box.setSpacing(7);

                layout.setCenter(box);

                validator.setValue(new ChainedValidator(List.of(d != null && d.getValidator() != null ? d.getValidator() : new SimpleValidator(), propVal)));
            } else {
                layout.setCenter(null);
                validator.setValue(new SimpleValidator());
            }
        });

        layout.setBottom(message.createRegion());

        var sep = new Separator();
        sep.getStyleClass().add("spacer");
        var top = new VBox(providerChoice.createRegion(), sep);
        top.getStyleClass().add("top");
        layout.setTop(top);
        // layout.getStyleClass().add("data-input-creation-step");
        return new LoadingOverlayComp(Comp.of(() -> layout), busy).createStructure();
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

        if (input.getValue() == null) {
            return false;
        }

        if (messageProp.getValue() != null && !changedSinceError.get()) {
            if (showInvalidConfirmAlert()) {
                return true;
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
            messageProp.setValue(msg);
            message.show();
            changedSinceError.setValue(false);
            return false;
        }

        ThreadHelper.runAsync(() -> {
            try (var b = new BusyProperty(busy)) {
                entry.getValue().setStore(input.getValue());
                entry.getValue().refresh(true);
                finished.setValue(true);
                PlatformThread.runLaterIfNeeded(parent::next);
            } catch (Exception ex) {
                messageProp.setValue(ExceptionConverter.convertMessage(ex));
                message.show();
                changedSinceError.setValue(false);
                ErrorEvent.fromThrowable(ex).omit().reportable(false).handle();
            }
        });
        return false;
    }
}
