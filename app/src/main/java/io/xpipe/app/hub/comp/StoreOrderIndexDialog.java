package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.base.*;
import io.xpipe.app.platform.OptionsBuilder;

import javafx.beans.property.SimpleObjectProperty;

public class StoreOrderIndexDialog {

    public static void show(StoreEntryWrapper wrapper) {
        var entry = wrapper.getEntry();
        var prop = new SimpleObjectProperty<>(
                entry.getOrderIndex() != 0
                                && entry.getOrderIndex() != Integer.MIN_VALUE
                                && entry.getOrderIndex() != Integer.MAX_VALUE
                        ? entry.getOrderIndex()
                        : null);
        var options = new OptionsBuilder()
                .nameAndDescription("orderIndex")
                .addComp(new IntFieldComp(prop, 0, Integer.MAX_VALUE))
                .buildComp()
                .prefWidth(400);
        var modal = ModalOverlay.of("changeOrderIndexTitle", options);
        modal.withDefaultButtons(() -> {
            if (prop.getValue() == null) {
                return;
            }

            wrapper.orderWithIndex(prop.getValue());
        });
        modal.show();
    }
}
