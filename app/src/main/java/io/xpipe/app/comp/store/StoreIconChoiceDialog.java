package io.xpipe.app.comp.store;

import io.xpipe.app.comp.base.*;
import io.xpipe.app.icon.SystemIcon;
import io.xpipe.app.icon.SystemIconManager;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.Hyperlinks;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;

public class StoreIconChoiceDialog {

    public static void show(DataStoreEntry entry) {
        var dialog = new StoreIconChoiceDialog(entry);
        dialog.getOverlay().show();
    }

    private final ObjectProperty<SystemIcon> selected = new SimpleObjectProperty<>();
    private final DataStoreEntry entry;

    @Getter
    private final ModalOverlay overlay;

    public StoreIconChoiceDialog(DataStoreEntry entry) {
        this.entry = entry;
        this.overlay = createOverlay();
    }

    private ModalOverlay createOverlay() {
        var filterText = new SimpleStringProperty();
        var filter = new FilterComp(filterText).grow(true, false);
        filter.focusOnShow();
        var github = new ButtonComp(null, new FontIcon("mdomz-settings"), () -> {
            overlay.close();
            AppPrefs.get().selectCategory("icons");
                })
                .grow(false, true);
        var modal = ModalOverlay.of(
                "chooseCustomIcon",
                new StoreIconChoiceComp(() -> {
                    overlay.close();
                    Platform.runLater(() -> overlay.show());
                }, selected, SystemIconManager.getIcons(), 5, filterText, () -> {
                            finish();
                        })
                        .prefWidth(600));
        modal.addButtonBarComp(github);
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
        entry.setIcon(selected.get() != null ? selected.getValue().getSource().getId() + "/" + selected.getValue().getId() : null, true);
        overlay.close();
    }
}
