package io.xpipe.app.comp.store;

import io.xpipe.app.comp.base.*;
import io.xpipe.app.resources.SystemIcon;
import io.xpipe.app.resources.SystemIcons;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.Hyperlinks;

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
        var github = new ButtonComp(null, new FontIcon("mdi2g-github"), () -> {
                    Hyperlinks.open(Hyperlinks.SELFHST_ICONS);
                })
                .grow(false, true);
        var modal = ModalOverlay.of(
                "chooseCustomIcon",
                new StoreIconChoiceComp(selected, SystemIcons.getSystemIcons(), 5, filterText, () -> {
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
        entry.setIcon(selected.get() != null ? selected.getValue().getIconName() : null, true);
        overlay.close();
    }
}
