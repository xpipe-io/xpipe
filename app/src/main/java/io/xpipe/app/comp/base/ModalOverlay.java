package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;

import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.util.LabelGraphic;
import lombok.*;
import lombok.experimental.NonFinal;

import java.util.ArrayList;
import java.util.List;

@Value
@With
@Builder(toBuilder = true)
public class ModalOverlay {

    public static ModalOverlay of(String titleKey, Comp<?> content) {
        return of(titleKey, content, null);
    }

    public static ModalOverlay of(String titleKey, Comp<?> content, LabelGraphic graphic) {
        return new ModalOverlay(titleKey, content, graphic, new ArrayList<>(), false, null);
    }

    public ModalOverlay withDefaultButtons(Runnable action) {
        addButton(ModalButton.cancel());
        addButton(ModalButton.ok(action));
        return this;
    }

    public ModalOverlay withDefaultButtons() {
        return withDefaultButtons(() -> {});
    }

    String titleKey;
    Comp<?> content;
    LabelGraphic graphic;

    @Singular
    List<Object> buttons;

    @NonFinal
    boolean persistent;

    @NonFinal
    @Setter
    Runnable onClose;

    public ModalButton addButton(ModalButton button) {
        buttons.add(button);
        return button;
    }

    public void addButtonBarComp(Comp<?> comp) {
        buttons.add(comp);
    }

    public void persist() {
        persistent = true;
    }

    public void showAndWait() {
        AppDialog.showAndWait(this);
    }

    public void close() {
        AppDialog.closeDialog(this);
    }
}
