package io.xpipe.app.comp.base;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;

import io.xpipe.app.core.window.AppDialog;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;

public class ModalOverlayStackComp extends SimpleRegionBuilder {

    private final BaseRegionBuilder<?, ?> background;

    public ModalOverlayStackComp(BaseRegionBuilder<?, ?> background) {
        this.background = background;
    }

    @Override
    protected Region createSimple() {
        var current = background;
        for (var i = 0; i < 6; i++) {
            current = buildModalOverlay(current, i);
        }
        return current.build();
    }

    private BaseRegionBuilder<?, ?> buildModalOverlay(BaseRegionBuilder<?, ?> current, int index) {
        var modalOverlays = AppDialog.getModalOverlaysRaw();
        var prop = new SimpleObjectProperty<>(modalOverlays.size() > index ? modalOverlays.get(index) : null);
        modalOverlays.addListener((ListChangeListener<? super ModalOverlay>) c -> {
            prop.set(modalOverlays.size() > index ? modalOverlays.get(index) : null);
        });
        prop.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                AppDialog.hide(oldValue);
            }
        });
        var comp = new ModalOverlayComp(current, prop);
        comp.style("modal-overlay-stack-element");
        return comp;
    }
}
