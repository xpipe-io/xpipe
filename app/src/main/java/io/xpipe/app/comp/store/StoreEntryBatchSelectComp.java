package io.xpipe.app.comp.store;

import io.xpipe.app.comp.SimpleComp;

import io.xpipe.app.util.BooleanScope;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.control.CheckBox;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

public class StoreEntryBatchSelectComp extends SimpleComp {

    private final StoreSection section;

    public StoreEntryBatchSelectComp(StoreSection section) {
        this.section = section;
    }

    @Override
    protected Region createSimple() {
        var selfUpdate = new SimpleBooleanProperty(false);
        var cb = new CheckBox();
        cb.setAllowIndeterminate(true);
        cb.selectedProperty().addListener((observable, oldValue, newValue) -> {
            BooleanScope.executeExclusive(selfUpdate, () -> {
                if (newValue) {
                    StoreViewState.get().selectBatchMode(section);
                } else {
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
                event.consume();
            }
        });
        return cb;
    }

    private void externalUpdate(CheckBox checkBox) {
        var isSelected = StoreViewState.get().isSectionSelected(section);
        checkBox.setSelected(isSelected);
        if (section.getShownChildren().getList().size() == 0) {
            checkBox.setIndeterminate(false);
            return;
        }

        var count = section.getShownChildren().getList().stream()
                .filter(c ->
                        StoreViewState.get().isBatchModeSelected(c.getWrapper()))
                .count();
        checkBox.setIndeterminate(
                count > 0 && count != section.getShownChildren().getList().size());
        return;
    }
}
