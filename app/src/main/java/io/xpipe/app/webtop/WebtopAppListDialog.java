package io.xpipe.app.webtop;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.concurrent.Semaphore;

public class WebtopAppListDialog {

    private static Semaphore semaphore = new Semaphore(1);
    private static List<WebtopApp> lastShown;

    public static void show(List<WebtopApp> include) {
        if (!semaphore.tryAcquire()) {
            return;
        }

        if (!include.isEmpty() && include.equals(lastShown)) {
            semaphore.release();
            return;
        }

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
        lastShown = include;
        semaphore.release();
    }
}
