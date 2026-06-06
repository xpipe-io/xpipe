package io.xpipe.app.webtop;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class WebtopAppListDialog {

    public static void show(List<WebtopApp> include) {
        ObservableList<WebtopApp> selected = FXCollections.observableArrayList();
        var m = WebtopAppListManager.get();
        selected.addAll(m.getSelected());
        for (WebtopApp wa : include) {
            if (!selected.contains(wa)) {
                selected.add(wa);
            }
        }
        var modal = ModalOverlay.of("webtopAppList", new WebtopAppListComp(selected).prefWidth(600).prefHeight(700));
        modal.addButton(ModalButton.cancel());
        modal.addButton(new ModalButton("install", () -> {
            ThreadHelper.runFailableAsync(() -> {
                WebtopAppListManager.get().install(selected);
            });
        }, true, true));
        modal.show();
    }
}
