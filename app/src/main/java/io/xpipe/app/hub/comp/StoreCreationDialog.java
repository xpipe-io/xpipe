package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.*;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class StoreCreationDialog {

    public static StoreCreationModel showEdit(DataStoreEntry e) {
        return showEdit(e, dataStoreEntry -> {});
    }

    public static StoreCreationModel showEdit(DataStoreEntry e, Consumer<DataStoreEntry> c) {
        return showEdit(e, e.getStore(), true, c);
    }

    public static StoreCreationModel showEdit(
            DataStoreEntry e, DataStore base, boolean addToStorage, Consumer<DataStoreEntry> c) {
        StoreCreationConsumer consumer = (newE, validated) -> {
            ThreadHelper.runAsync(() -> {
                if (!addToStorage) {
                    DataStorage.get().updateEntry(e, newE);
                    c.accept(e);
                    return;
                }

                if (!DataStorage.get().getStoreEntries().contains(e)
                        || DataStorage.get().getEffectiveReadOnlyState(e)) {
                    DataStorage.get().addStoreEntryIfNotPresent(newE);
                } else {
                    // We didn't change anything
                    if (e.getStore().equals(newE.getStore())) {
                        e.setName(newE.getName());
                    } else {
                        var madeValid = !e.getValidity().isUsable()
                                && newE.getValidity().isUsable();
                        DataStorage.get().updateEntry(e, newE);
                        if (madeValid
                                && validated
                                && e.getProvider().shouldShowScan()
                                && AppPrefs.get()
                                        .openConnectionSearchWindowOnConnectionCreation()
                                        .get()) {
                            ScanDialog.showSingleAsync(e);
                        }
                    }
                }

                // Select new category if needed
                var cat = DataStorage.get()
                        .getStoreCategoryIfPresent(e.getCategoryUuid())
                        .orElseThrow();
                PlatformThread.runLaterIfNeeded(() -> {
                    StoreViewState.get()
                            .selectCategoryIntoViewIfNeeded(StoreViewState.get().getCategoryWrapper(cat));
                });

                c.accept(e);
            });
        };
        return show(e.getName(), DataStoreProviders.byStore(base), base, v -> true, consumer, true, e);
    }

    public static StoreCreationModel showCreation(DataStoreProvider selected, DataStoreCreationCategory category) {
        return showCreation(
                null,
                selected != null ? selected.defaultStore(DataStorage.get().getSelectedCategory()) : null,
                category,
                dataStoreEntry -> {},
                true);
    }

    public static StoreCreationModel showCreation(
            String name,
            DataStore base,
            DataStoreCreationCategory category,
            Consumer<DataStoreEntry> listener,
            boolean selectCategory) {
        var prov = base != null ? DataStoreProviders.byStore(base) : null;
        StoreCreationConsumer consumer = (e, validated) -> {
            try {
                var returned = DataStorage.get().addStoreEntryIfNotPresent(e);
                listener.accept(returned);
                if (validated
                        && e.getProvider().shouldShowScan()
                        && AppPrefs.get()
                                .openConnectionSearchWindowOnConnectionCreation()
                                .get()) {
                    ScanDialog.showSingleAsync(e);
                }

                if (selectCategory) {
                    // Select new category if needed
                    var cat = DataStorage.get()
                            .getStoreCategoryIfPresent(e.getCategoryUuid())
                            .orElseThrow();
                    PlatformThread.runLaterIfNeeded(() -> {
                        StoreViewState.get()
                                .selectCategoryIntoViewIfNeeded(
                                        StoreViewState.get().getCategoryWrapper(cat));
                    });
                }
            } catch (Exception ex) {
                ErrorEventFactory.fromThrowable(ex).handle();
            }
        };
        return show(
                name,
                prov,
                base,
                dataStoreProvider -> (category != null
                                && dataStoreProvider.allowCreation()
                                && category.equals(dataStoreProvider.getCreationCategory()))
                        || dataStoreProvider.equals(prov),
                consumer,
                false,
                null);
    }

    private static StoreCreationModel show(
            String initialName,
            DataStoreProvider provider,
            DataStore s,
            Predicate<DataStoreProvider> filter,
            StoreCreationConsumer con,
            boolean staticDisplay,
            DataStoreEntry existingEntry) {
        var ex = StoreCreationQueueEntry.findExisting(existingEntry);
        if (ex.isPresent()) {
            ex.get().execute();
            return null;
        }

        var prop = new SimpleObjectProperty<>(provider);
        var store = new SimpleObjectProperty<>(s);
        var model = new StoreCreationModel(prop, store, filter, initialName, existingEntry, staticDisplay, con);
        var modal = createModalOverlay(model);
        modal.show();
        return model;
    }

    private static ModalOverlay createModalOverlay(StoreCreationModel model) {
        var comp = new StoreCreationComp(model);
        comp.prefWidth(650);
        var nameKey = model.isQuickConnect() ? "quickConnect" : model.storeTypeNameKey() + "Add";
        var modal = ModalOverlay.of(nameKey, comp);
        comp.apply(struc -> {
            struc.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    var changed = model.hasBeenModified();
                    if (!changed) {
                        modal.close();
                        e.consume();
                    }
                }
            });
        });

        if (!model.isQuickConnect()) {
            var queueEntry = StoreCreationQueueEntry.of(model, modal);
            modal.hideable(queueEntry);
            AppLayoutModel.get().getSelected().addListener((observable, oldValue, newValue) -> {
                if (model.getFinished().get() || !modal.isShowing()) {
                    return;
                }

                modal.hide();
                AppLayoutModel.get().getQueueEntries().add(queueEntry);
            });
            modal.setRequireCloseButtonForClose(true);
        }

        var loadingLabel = new LabelComp(Bindings.createStringBinding(
                () -> {
                    return model.getBusy().get() ? AppI18n.get("testingConnection") : null;
                },
                model.getBusy(),
                AppI18n.activeLanguage()));
        modal.addButtonBarComp(loadingLabel);
        modal.addButtonBarComp(RegionBuilder.hspacer());
        modal.addButton(new ModalButton(
                        "docs",
                        () -> {
                            model.showDocs();
                        },
                        false,
                        false)
                .augment(button -> {
                    button.visibleProperty().bind(Bindings.not(model.canShowDocs()));
                }));
        modal.addButton(new ModalButton(
                        "connect",
                        () -> {
                            model.connect();
                        },
                        false,
                        false)
                .augment(button -> {
                    button.visibleProperty().bind(Bindings.not(model.canConnect()));
                }));
        if (!model.isQuickConnect()) {
            modal.addButton(new ModalButton(
                            "skip",
                            () -> {
                                model.commit(false);
                                modal.close();
                            },
                            false,
                            false))
                    .augment(button -> {
                        button.visibleProperty().bind(model.getSkippable());
                        button.disableProperty().bind(model.getBusy());
                    });
        }

        modal.addButton(new ModalButton(
                        model.isQuickConnect() ? "connect" : "finish",
                        () -> {
                            model.finish();
                        },
                        false,
                        true))
                .augment(button -> {
                    button.graphicProperty()
                            .bind(Bindings.createObjectBinding(
                                    () -> {
                                        return model.getBusy().get()
                                                ? new LoadingIconComp(model.getBusy(), AppFontSizes::base)
                                                        .style("store-creator-busy")
                                                        .build()
                                                : null;
                                    },
                                    PlatformThread.sync(model.getBusy())));
                    button.textProperty()
                            .bind(Bindings.createStringBinding(
                                    () -> {
                                        return !model.getBusy().get()
                                                ? AppI18n.get(model.isQuickConnect() ? "connect" : "finish")
                                                : null;
                                    },
                                    PlatformThread.sync(model.getBusy()),
                                    AppI18n.activeLanguage()));
                });
        model.getFinished().addListener((obs, oldValue, newValue) -> {
            modal.close();
        });
        return modal;
    }
}
