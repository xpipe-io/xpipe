package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.base.*;
import io.xpipe.app.icon.SystemIcon;
import io.xpipe.app.icon.SystemIconManager;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

public class StoreIconChoiceDialog {

    private final ObjectProperty<SystemIcon> selected = new SimpleObjectProperty<>();
    private final String defaultIcon;
    private final Consumer<SystemIcon> iconApply;

    @Getter
    private final ModalOverlay overlay;

    public StoreIconChoiceDialog(String defaultIcon, Consumer<SystemIcon> iconApply) {
        this.defaultIcon = defaultIcon;
        this.iconApply = iconApply;
        this.overlay = createOverlay();
    }

    public static void show(DataStoreEntry entry) {
        var dialog = new StoreIconChoiceDialog(entry.getProvider().getDisplayIconFileName(entry.getStore()), s -> {
            entry.setIcon(s != null && s.getSource() != null ? s.getSource().getId() + "/" + s.getId() : null, true);
        });
        dialog.getOverlay().show();
    }

    public static void show(DataStoreCategory entry) {
        var dialog = new StoreIconChoiceDialog(entry.getDefaultIconFile(), s -> {
            entry.setIcon(s != null && s.getSource() != null ? s.getSource().getId() + "/" + s.getId() : null, true);
        });
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

        var comp = new StoreIconChoiceComp(
                selected,
                SystemIconManager.getIcons(),
                5,
                filterText,
                () -> {
                    finish();
                },
                defaultIcon);
        comp.prefWidth(600);

        var modal = ModalOverlay.of("chooseCustomIcon", comp);
        var refresh = new ButtonComp(null, new FontIcon("mdi2r-refresh"), () -> {
                    comp.refresh();
                })
                .maxHeight(100)
                .disable(comp.getBusy());
        var settings = new ButtonComp(null, new FontIcon("mdomz-settings"), () -> {
                    overlay.close();
                    AppPrefs.get().selectCategory("icons");
                })
                .disable(comp.getBusy())
                .maxHeight(100);
        modal.addButtonBarComp(settings);
        modal.addButtonBarComp(refresh);
        modal.addButtonBarComp(filter);
        modal.addButton(ModalButton.ok(() -> {
                    finish();
                }))
                .augment(button -> button.disableProperty()
                        .bind(Bindings.createBooleanBinding(
                                () -> {
                                    return selected.get() == null
                                            || comp.getBusy().get();
                                },
                                selected,
                                PlatformThread.sync(comp.getBusy()))));
        return modal;
    }

    private void finish() {
        iconApply.accept(selected.get());
        overlay.close();
    }
}
