package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;

public class ModalOverlayStackComp extends SimpleComp {

    private final Comp<?> background;
    private final ObservableList<ModalOverlay> modalOverlay;

    public ModalOverlayStackComp(Comp<?> background, ObservableList<ModalOverlay> modalOverlay) {
        this.background = background;
        this.modalOverlay = modalOverlay;
    }

    @Override
    protected Region createSimple() {
        var current = background;
        for (var i = 0; i < 5; i++) {
            current = buildModalOverlay(current, i);
        }
        return current.createRegion();
    }

    private Comp<?> buildModalOverlay(Comp<?> current, int index) {
        var prop = new SimpleObjectProperty<ModalOverlay>();
        modalOverlay.subscribe(() -> {
            var ex = prop.get();
            // Don't shift just for an index change
            if (ex != null && modalOverlay.contains(ex)) {
                return;
            }

            prop.set(modalOverlay.size() > index ? modalOverlay.get(index) : null);
        });
        prop.addListener((observable, oldValue, newValue) -> {
            if (newValue == null && modalOverlay.indexOf(oldValue) == index) {
                modalOverlay.remove(oldValue);
            }
        });
        var comp = new ModalOverlayComp(current, prop);
        return comp;
    }
}
