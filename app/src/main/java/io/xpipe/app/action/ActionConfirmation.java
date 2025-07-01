package io.xpipe.app.action;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;

import javafx.beans.property.SimpleBooleanProperty;

import java.util.List;

public class ActionConfirmation {

    public static boolean confirmAction(AbstractAction action) {
        if (!action.forceConfirmation() && (!action.isMutation() || !confirmAllModifications(action))) {
            return true;
        }

        var ok = new SimpleBooleanProperty(false);
        var modal = ModalOverlay.of("confirmAction", new ActionConfirmComp(action).prefWidth(550));
        modal.addButton(ModalButton.cancel());
        modal.addButton(ModalButton.ok(() -> ok.set(true)));
        modal.showAndWait();
        return ok.get();
    }

    private static boolean confirmAllModifications(AbstractAction action) {
        var context = getContext(action);
        return context.stream().anyMatch(dataStoreEntry -> {
            var config = DataStorage.get().getEffectiveCategoryConfig(dataStoreEntry);
            return config.getConfirmAllModifications() != null && config.getConfirmAllModifications();
        });
    }

    private static List<DataStoreEntry> getContext(AbstractAction action) {
        if (action instanceof StoreContextAction ca) {
            return ca.getStoreEntryContext();
        }

        return List.of();
    }
}
