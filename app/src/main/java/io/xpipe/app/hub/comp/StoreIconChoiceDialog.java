package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.base.*;
import io.xpipe.app.icon.SystemIcon;
import io.xpipe.app.icon.SystemIconManager;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreEntry;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;

public class StoreIconChoiceDialog {

    private final ObjectProperty<SystemIcon> selected = new SimpleObjectProperty<>();
    private final DataStoreEntry entry;

    @Getter
    private final ModalOverlay overlay;

    public StoreIconChoiceDialog(DataStoreEntry entry) {
        this.entry = entry;
        this.overlay = createOverlay();
    }

    public static void show(DataStoreEntry entry) {
        var dialog = new StoreIconChoiceDialog(entry);
        dialog.getOverlay().show();
    }

    private ModalOverlay createOverlay() {
        var filterText = new SimpleStringProperty();
        var filter = new FilterComp(filterText).hgrow();
        // Ugly solution to focus the filter on show
        filter.apply(r -> {
            r.sceneProperty().subscribe(s -> {
                if (s != null) {
                    Platform.runLater(() -> {
                        Platform.runLater(() -> {
                            r.requestFocus();
                        });
                    });
                }
            });
        });
        var settings = new ButtonComp(null, new FontIcon("mdomz-settings"), () -> {
                    overlay.close();
                    AppPrefs.get().selectCategory("icons");
                })
                .maxHeight(100);
        var modal = ModalOverlay.of(
                "chooseCustomIcon",
                new StoreIconChoiceComp(
                                () -> {
                                    var showing = overlay.isShowing();
                                    overlay.close();
                                    if (showing) {
                                        Platform.runLater(() -> overlay.show());
                                    }
                                },
                                selected,
                                SystemIconManager.getIcons(),
                                5,
                                filterText,
                                () -> {
                                    finish();
                                })
                        .prefWidth(600));
        modal.addButtonBarComp(settings);
        modal.addButtonBarComp(filter);
        modal.addButton(new ModalButton(
                "clear",
                () -> {
                    selected.setValue(null);
                    finish();
                },
                true,
                false));
        modal.addButton(ModalButton.ok(() -> {
                    finish();
                }))
                .augment(button -> button.disableProperty().bind(selected.isNull()));
        return modal;
    }

    private void finish() {
        entry.setIcon(
                selected.get() != null
                        ? selected.getValue().getSource().getId() + "/"
                                + selected.getValue().getId()
                        : null,
                true);
        overlay.close();
    }
}
