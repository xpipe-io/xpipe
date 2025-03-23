package io.xpipe.app.comp.store;

import io.xpipe.app.comp.base.DialogComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.*;
import io.xpipe.core.store.DataStore;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import org.bouncycastle.math.raw.Mod;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class StoreCreationDialog {

    public static void showEdit(DataStoreEntry e) {
        showEdit(e, dataStoreEntry -> {});
    }

    public static void showEdit(DataStoreEntry e, Consumer<DataStoreEntry> consumer) {
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
                                var madeValid = !e.getValidity().isUsable()
                                        && newE.getValidity().isUsable();
                                DataStorage.get().updateEntry(e, newE);
                                if (madeValid) {
                                    StoreViewState.get().toggleStoreListUpdate();
                                }
                            }
                        }
                        consumer.accept(e);
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
        var model = new StoreCreationModel(prop, store, filter, initialName, existingEntry, staticDisplay);
        var modal = createModalOverlay(model);
        modal.show();
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

    private static ModalOverlay createModalOverlay(StoreCreationModel model) {
        var comp = new StoreCreationComp(model);
        comp.prefWidth(650);
        var nameKey = model.storeTypeNameKey() + "Add";
        var modal = ModalOverlay.of(nameKey, comp);
        modal.persist();
        modal.addButton(new ModalButton("docs", () -> {
            model.showDocs();
        }, false, false).augment(button -> {
            button.visibleProperty().bind(Bindings.not(model.canShowDocs()));
        }));
        modal.addButton(ModalButton.cancel());
        var graphic = model.getProvider().getValue() != null ?
                new LabelGraphic.ImageGraphic(model.getProvider().getValue().getDisplayIconFileName(null), 20) :
                new LabelGraphic.IconGraphic("mdi2b-beaker-plus-outline");
        modal.addButton(ModalButton.hide(AppI18n.observable(model.storeTypeNameKey() + "Add"), graphic, () -> {
            modal.show();
        }));
        modal.addButton(new ModalButton("connect", () -> {
            model.connect();
        }, false, false).augment(button -> {
            button.visibleProperty().bind(Bindings.not(model.canConnect()));
        }));
        modal.addButton(new ModalButton("skipValidation", () -> {
            if (showInvalidConfirmAlert()) {
                model.commit();
            } else {
                model.finish();
            }
        }, true, false));
        modal.addButton(new ModalButton("finish", () -> {
            model.finish();
        }, true, true));
        return modal;
    }
}
