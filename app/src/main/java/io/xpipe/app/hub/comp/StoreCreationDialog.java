package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.*;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class StoreCreationDialog {

    public static void showEdit(DataStoreEntry e) {
        showEdit(e, dataStoreEntry -> {});
    }

    public static void showEdit(DataStoreEntry e, Consumer<DataStoreEntry> c) {
        showEdit(e, e.getStore(), c);
    }

    public static void showEdit(DataStoreEntry e, DataStore base, Consumer<DataStoreEntry> c) {
        StoreCreationConsumer consumer = (newE, validated) -> {
            ThreadHelper.runAsync(() -> {
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
                        if (madeValid) {
                            if (validated
                                    && e.getProvider().shouldShowScan()
                                    && AppPrefs.get()
                                            .openConnectionSearchWindowOnConnectionCreation()
                                            .get()) {
                                ScanDialog.showSingleAsync(e);
                            }
                        }
                    }
                }

                // Select new category if needed
                var cat = DataStorage.get()
                        .getStoreCategoryIfPresent(e.getCategoryUuid())
                        .orElseThrow();
                PlatformThread.runLaterIfNeeded(() -> {
                    StoreViewState.get()
                            .getActiveCategory()
                            .setValue(StoreViewState.get().getCategoryWrapper(cat));
                });

                c.accept(e);
            });
        };
        show(e.getName(), DataStoreProviders.byStore(base), base, v -> true, consumer, true, e);
    }

    public static void showCreation(DataStoreProvider selected, DataStoreCreationCategory category) {
        showCreation(
                selected != null ? selected.defaultStore(DataStorage.get().getSelectedCategory()) : null,
                category,
                dataStoreEntry -> {},
                true);
    }

    public static void showCreation(
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
                                .getActiveCategory()
                                .setValue(StoreViewState.get().getCategoryWrapper(cat));
                    });
                }
            } catch (Exception ex) {
                ErrorEventFactory.fromThrowable(ex).handle();
            }
        };
        show(
                null,
                prov,
                base,
                dataStoreProvider -> (category != null && category.equals(dataStoreProvider.getCreationCategory()))
                        || dataStoreProvider.equals(prov),
                consumer,
                false,
                null);
    }

    private static void show(
            String initialName,
            DataStoreProvider provider,
            DataStore s,
            Predicate<DataStoreProvider> filter,
            StoreCreationConsumer con,
            boolean staticDisplay,
            DataStoreEntry existingEntry) {
        var ex = StoreCreationQueueEntry.findExisting(existingEntry);
        if (ex.isPresent()) {
            ex.get().show();
            return;
        }

        var prop = new SimpleObjectProperty<>(provider);
        var store = new SimpleObjectProperty<>(s);
        var model = new StoreCreationModel(prop, store, filter, initialName, existingEntry, staticDisplay, con);
        var modal = createModalOverlay(model);
        modal.show();
    }

    private static ModalOverlay createModalOverlay(StoreCreationModel model) {
        var comp = new StoreCreationComp(model);
        comp.prefWidth(650);
        var nameKey = model.storeTypeNameKey() + "Add";
        var modal = ModalOverlay.of(nameKey, comp);
        var queueEntry = StoreCreationQueueEntry.of(model, modal);
        comp.apply(struc -> {
            struc.get().addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    var changed = model.hasBeenModified();
                    if (!changed) {
                        modal.close();
                        e.consume();
                    }
                }
            });
        });
        modal.hideable(queueEntry);
        AppLayoutModel.get().getSelected().addListener((observable, oldValue, newValue) -> {
            if (model.getFinished().get() || !modal.isShowing()) {
                return;
            }

            modal.hide();
            AppLayoutModel.get()
                    .getQueueEntries()
                    .add(queueEntry);
        });
        modal.setRequireCloseButtonForClose(true);
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
                });
        modal.addButton(new ModalButton(
                "finish",
                () -> {
                    model.finish();
                },
                false,
                true));
        model.getFinished().addListener((obs, oldValue, newValue) -> {
            modal.close();
        });
        return modal;
    }
}
