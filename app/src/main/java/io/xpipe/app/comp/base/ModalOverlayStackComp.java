package io.xpipe.app.comp.base;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;

import java.util.concurrent.atomic.AtomicInteger;

public class ModalOverlayStackComp extends SimpleRegionBuilder {

    private final BaseRegionBuilder<?, ?> background;
    private final ObservableList<ModalOverlay> modalOverlay;

    public ModalOverlayStackComp(BaseRegionBuilder<?, ?> background, ObservableList<ModalOverlay> modalOverlay) {
        this.background = background;
        this.modalOverlay = modalOverlay;
    }

    @Override
    protected Region createSimple() {
        var current = background;
        for (var i = 0; i < 5; i++) {
            current = buildModalOverlay(current, i);
        }
        return current.build();
    }

    private BaseRegionBuilder<?, ?> buildModalOverlay(BaseRegionBuilder<?, ?> current, int index) {
        AtomicInteger currentIndex = new AtomicInteger(index);
        var prop = new SimpleObjectProperty<>(modalOverlay.size() > index ? modalOverlay.get(index) : null);
        modalOverlay.addListener((ListChangeListener<? super ModalOverlay>) c -> {
            var ex = prop.get();
            // Don't shift just for an index change
            if (ex != null && modalOverlay.contains(ex)) {
                currentIndex.set(modalOverlay.indexOf(ex));
                return;
            } else {
                currentIndex.set(index);
            }

            prop.set(modalOverlay.size() > index ? modalOverlay.get(index) : null);
        });
        prop.addListener((observable, oldValue, newValue) -> {
            if (newValue == null && modalOverlay.indexOf(oldValue) == currentIndex.get()) {
                modalOverlay.remove(oldValue);
            }
        });
        var comp = new ModalOverlayComp(current, prop);
        comp.style("modal-overlay-stack-element");
        return comp;
    }
}
