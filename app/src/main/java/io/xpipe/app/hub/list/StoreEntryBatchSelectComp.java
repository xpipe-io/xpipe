package io.xpipe.app.hub.list;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.hub.entry.StoreEntryWrapper;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.BooleanScope;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.control.CheckBox;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

public class StoreEntryBatchSelectComp extends SimpleRegionBuilder {

    private final StoreSection section;

    public StoreEntryBatchSelectComp(StoreSection section) {
        this.section = section;
    }

    @Override
    protected Region createSimple() {
        var selfUpdate = new SimpleBooleanProperty(false);
        var cb = new CheckBox();
        externalUpdate(cb);
        cb.setAllowIndeterminate(true);
        cb.selectedProperty().addListener((observable, oldValue, newValue) -> {
            BooleanScope.executeExclusive(selfUpdate, () -> {
                if (newValue) {
                    StoreViewState.get().selectBatchMode(section);
                } else {
                    if (section.getWrapper() == null && cb.isIndeterminate()) {
                        return;
                    }

                    StoreViewState.get().unselectBatchMode(section);
                }
            });
        });

        StoreViewState.get().getBatchModeSelection().getList().addListener((ListChangeListener<
                        ? super StoreEntryWrapper>)
                c -> {
                    if (selfUpdate.get()) {
                        return;
                    }

                    Platform.runLater(() -> {
                        externalUpdate(cb);
                    });
                });
        section.getShownChildren().getList().addListener((ListChangeListener<? super StoreSection>) c -> {
            if (selfUpdate.get()) {
                return;
            }

            BooleanScope.executeExclusive(selfUpdate, () -> {
                if (cb.isSelected()) {
                    StoreViewState.get().selectBatchMode(section);
                }
            });
        });

        cb.getStyleClass().add("batch-mode-selector");
        cb.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                cb.setSelected(!cb.isSelected());

                if (section.getWrapper() == null) {
                    cb.setIndeterminate(false);
                    if (cb.isSelected()) {
                        StoreViewState.get().selectBatchMode(section);
                    } else {
                        StoreViewState.get().unselectBatchMode(section);
                    }
                }

                event.consume();
            }
        });
        return cb;
    }

    private void externalUpdate(CheckBox checkBox) {
        if (section.getWrapper() != null && section.getEntry().getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
            checkBox.setSelected(false);
            checkBox.setIndeterminate(false);
            checkBox.setDisable(true);
            return;
        }

        var count = section.getShownChildren().getList().stream()
                .filter(c -> StoreViewState.get().isBatchModeSelected(c.getWrapper()))
                .count();
        var mixedSelection =
                count > 0 && count != section.getShownChildren().getList().size();

        var isSelected = section.getWrapper() == null
                ? count > 0 && !mixedSelection
                : StoreViewState.get().isBatchModeSelected(section.getWrapper());

        checkBox.setIndeterminate(mixedSelection);
        checkBox.setSelected(isSelected);
    }
}
