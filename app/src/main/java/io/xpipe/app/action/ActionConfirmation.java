package io.xpipe.app.action;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;

import javafx.beans.property.SimpleBooleanProperty;

import java.util.List;

public class ActionConfirmation {

    public static boolean confirmAction(AbstractAction action) {
        var ok = new SimpleBooleanProperty(false);
        var modal = ModalOverlay.of("confirmAction", new ActionConfirmComp(action).prefWidth(550));
        modal.addButton(ModalButton.cancel());
        modal.addButton(ModalButton.ok(() -> ok.set(true)));
        modal.showAndWait();
        return ok.get();
    }
}
