package io.xpipe.app.hub.comp;

import io.xpipe.app.browser.action.impl.ChgrpActionProvider;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.icon.SystemIcon;
import io.xpipe.app.icon.SystemIconManager;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.OptionsBuilder;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TextField;
import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;

public class StoreOrderIndexDialog {

    public static void show(DataStoreEntry entry) {
        var prop = new SimpleObjectProperty<>(
                entry.getOrderIndex() != 0 && entry.getOrderIndex() != Integer.MIN_VALUE && entry.getOrderIndex() != Integer.MAX_VALUE ?
                entry.getOrderIndex() : null);
        var options = new OptionsBuilder()
                .nameAndDescription("orderIndex")
                .addComp(new IntFieldComp(prop, 0, Integer.MAX_VALUE))
                .buildComp()
                .prefWidth(400);
        var modal = ModalOverlay.of(
                "changeOrderIndexTitle",
                options);
        modal.withDefaultButtons(() -> {
            if (prop.getValue() == null) {
                return;
            }

            DataStorage.get().setOrderIndex(entry, prop.getValue());
        });
        modal.show();
    }
}
