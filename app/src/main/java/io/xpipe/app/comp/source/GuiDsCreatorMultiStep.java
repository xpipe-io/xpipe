package io.xpipe.app.comp.source;

import io.xpipe.app.comp.base.MultiStepComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.storage.DataSourceCollection;
import io.xpipe.app.storage.DataSourceEntry;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.DataSourceProviders;
import io.xpipe.extension.I18n;
import io.xpipe.extension.event.ErrorEvent;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.control.Alert;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class GuiDsCreatorMultiStep<DI extends DataStore, DS extends DataSource<DI>> extends MultiStepComp {

    private final Stage window;
    private final DataSourceEntry editing;
    private final DataSourceCollection targetGroup;
    private final Property<DataSourceType> dataSourceType;
    private final DataSourceProvider.Category category;
    private final Property<DataSourceProvider<?>> provider;
    private final Property<DI> store;
    private final ObjectProperty<DS> baseSource;
    private final ObjectProperty<DS> source;
    private final BooleanProperty loading = new SimpleBooleanProperty();
    private final State state;

    private GuiDsCreatorMultiStep(
            Stage window,
            DataSourceEntry editing, DataSourceCollection targetGroup,
            DataSourceProvider.Category category,
            DataSourceProvider<?> provider,
            DI store,
            DS source,
            State state) {
        this.window = window;
        this.editing = editing;
        this.targetGroup = targetGroup;
        this.category = category;
        this.provider = new SimpleObjectProperty<>(provider);
        this.store = new SimpleObjectProperty<>(store);
        this.dataSourceType = new SimpleObjectProperty<>(provider != null ? provider.getPrimaryType() : null);
        this.baseSource = new SimpleObjectProperty<>(source);
        this.source = new SimpleObjectProperty<>(source);
        this.state = state;

        addListeners();

        this.apply(r -> {
            r.get().setPrefWidth(AppFont.em(30));
            r.get().setPrefHeight(AppFont.em(35));
        });
    }

    public static void showCreation(DataSourceProvider.Category category, DataSourceCollection sourceCollection) {
        Platform.runLater(() -> {
            var loading = new SimpleBooleanProperty();
            var stage = AppWindowHelper.sideWindow(
                    I18n.get("newDataSource"),
                    window -> {
                        var ms = new GuiDsCreatorMultiStep<>(
                                window,
                                null, sourceCollection,
                                category,
                                null,
                                null,
                                null,
                                State.CREATE);
                        loading.bind(ms.loading);
                        window.setOnCloseRequest(e -> {
                            if (ms.state == State.CREATE && ms.source.getValue() != null) {
                                e.consume();
                                showCloseConfirmAlert(ms, window);
                            }
                        });
                        return ms;
                    },
                    false,
                    loading);
            stage.show();
        });
    }

    public static void showEdit(DataSourceEntry entry) {
        Platform.runLater(() -> {
            var loading = new SimpleBooleanProperty();
            var stage = AppWindowHelper.sideWindow(
                    I18n.get("editDataSource"),
                    window -> {
                        var ms = new GuiDsCreatorMultiStep<>(
                                window,
                                entry, DataStorage.get()
                                        .getCollectionForSourceEntry(entry)
                                        .orElse(null),
                                entry.getProvider().getCategory(),
                                entry.getProvider(),
                                entry.getStore().asNeeded(),
                                entry.getSource().asNeeded(),
                                State.EDIT);
                        loading.bind(ms.loading);
                        return ms.apply(struc -> ms.next());
                    },
                    false,
                    loading);
            stage.show();
        });
    }

    public static Future<Boolean> showForStore(
            DataSourceProvider.Category category, DataStore store, DataSourceCollection sourceCollection) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

        var provider = DataSourceProviders.byPreferredStore(store, null);
        Platform.runLater(() -> {
            var stage = AppWindowHelper.sideWindow(
                    I18n.get("newDataSource"),
                    window -> {
                        var gui = new GuiDsCreatorMultiStep<>(
                                window,
                                null, sourceCollection,
                                category,
                                provider.orElse(null),
                                store,
                                null,
                                State.CREATE);
                        gui.completedProperty().addListener((c, o, n) -> {
                            if (n) {
                                completableFuture.complete(true);
                            }
                        });
                        window.setOnCloseRequest(e -> {
                            if (gui.state == State.CREATE && gui.source.getValue() != null) {
                                e.consume();
                                showCloseConfirmAlert(gui, window);
                            }
                        });
                        return gui;
                    },
                    false,
                    null);
            stage.show();
            stage.setOnHiding(e -> {
                completableFuture.complete(false);
            });
        });

        return completableFuture;
    }

    private static void showCloseConfirmAlert(GuiDsCreatorMultiStep<?, ?> ms, Stage s) {
        AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(I18n.get("confirmDsCreationAbortTitle"));
                    alert.setHeaderText(I18n.get("confirmDsCreationAbortHeader"));
                    alert.setContentText(I18n.get("confirmDsCreationAbortContent"));
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                })
                .filter(b -> b.getButtonData().isDefaultButton())
                .ifPresent(t -> {
                    s.close();
                });
    }

    @SuppressWarnings("unchecked")
    private void addListeners() {
        this.provider.addListener((c, o, n) -> {
            if (n == null) {
                this.dataSourceType.setValue(null);
                return;
            }

            if (baseSource.getValue() != null
                    && !n.getSourceClass().equals(baseSource.get().getClass())) {
                this.baseSource.setValue(null);
            }

            this.dataSourceType.setValue(n.getPrimaryType());
        });

        this.store.addListener((c, o, n) -> {
            if (n == null) {
                return;
            }

            if (this.provider.getValue() == null) {
                this.provider.setValue(
                        DataSourceProviders.byPreferredStore(n, null).orElse(null));
                if (this.provider.getValue() != null) {
                    try {
                        this.baseSource.set((DS) provider.getValue().createDefaultSource(n));
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).handle();
                    }
                }
            }
        });

        this.baseSource.addListener((c, o, n) -> {
            if (n == null) {
                this.source.set(null);
                return;
            }

            try {
                var converted = dataSourceType.getValue() != provider.getValue().getPrimaryType()
                        ? (provider.getValue()).convert(n.asNeeded(), dataSourceType.getValue())
                        : n;
                source.setValue((DS) converted);
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).handle();
            }
        });

        this.dataSourceType.addListener((c, o, n) -> {
            if (n == null || source.get() == null) {
                return;
            }

            if (n == this.provider.getValue().getPrimaryType()) {
                this.source.set(baseSource.getValue());
                return;
            }

            try {
                var conv = this.provider.getValue().convert(baseSource.get().asNeeded(), n);
                this.source.set(conv.asNeeded());
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).handle();
            }
        });
    }

    @Override
    protected Region createStepOverview(Region content) {
        var r = super.createStepOverview(content);
        AppFont.small(r);
        return r;
    }

    @Override
    protected Region createStepNavigation() {
        var r = super.createStepNavigation();
        AppFont.small(r);
        return r;
    }

    @Override
    protected List<Entry> setup() {
        var list = new ArrayList<Entry>();
        list.add(new Entry(I18n.observable("selectInput"), createInputStep()));
        list.add(new Entry(
                I18n.observable("configure"),
                new GuiDsConfigStep(provider, store, baseSource, source, dataSourceType, loading)));
        switch (state) {
            case EDIT -> {}
            case CREATE -> {
                list.add(new Entry(I18n.observable("target"), new GuiDsCreatorTransferStep(targetGroup, store, source)));
            }
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private MultiStepComp.Step<?> createInputStep() {
        return new GuiDsStoreSelectStep(
                this, provider, (ObjectProperty<DataStore>) store, category, baseSource, loading);
    }

    @Override
    protected void finish() {
        switch (state) {
            case EDIT -> {
                editing.setSource(source.get());
            }
            case CREATE -> {}
        }
        window.close();
    }

    public static enum State {
        EDIT,
        CREATE
    }
}
