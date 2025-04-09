package io.xpipe.app.comp.store;

import io.xpipe.app.comp.SimpleComp;

import javafx.application.Platform;
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
        var cb = new CheckBox();
        cb.setAllowIndeterminate(true);
        cb.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                StoreViewState.get().selectBatchMode(section);
            } else {
                StoreViewState.get().unselectBatchMode(section);
            }
        });

        StoreViewState.get().getBatchModeSelection().getList().addListener((ListChangeListener<
                        ? super StoreEntryWrapper>)
                c -> {
                    Platform.runLater(() -> {
                        update(cb);
                    });
                });
        section.getShownChildren().getList().addListener((ListChangeListener<? super StoreSection>) c -> {
            if (cb.isSelected()) {
                StoreViewState.get().selectBatchMode(section);
            }
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

    private void update(CheckBox checkBox) {
        var isSelected = StoreViewState.get().isSectionSelected(section);
        checkBox.setSelected(isSelected);
        if (section.getShownChildren().getList().size() == 0) {
            checkBox.setIndeterminate(false);
            return;
        }

        var count = section.getShownChildren().getList().stream()
                .filter(c ->
                        StoreViewState.get().getBatchModeSelection().getList().contains(c.getWrapper()))
                .count();
        checkBox.setIndeterminate(
                count > 0 && count != section.getShownChildren().getList().size());
        return;
    }
}
