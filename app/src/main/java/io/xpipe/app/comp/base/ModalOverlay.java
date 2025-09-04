package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.platform.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.*;
import lombok.experimental.NonFinal;

import java.util.ArrayList;
import java.util.List;

@Value
@With
@Builder(toBuilder = true)
public class ModalOverlay {

    ObservableValue<String> title;
    Comp<?> content;
    LabelGraphic graphic;

    @Singular
    List<Object> buttons;

    @NonFinal
    @Setter
    boolean hasCloseButton;

    @NonFinal
    @Setter
    boolean requireCloseButtonForClose;

    @NonFinal
    @Setter
    Runnable hideAction;

    public static ModalOverlay of(Comp<?> content) {
        return of((ObservableValue<String>) null, content, null);
    }

    public static ModalOverlay of(String titleKey, Comp<?> content) {
        return of(titleKey, content, null);
    }

    public static ModalOverlay of(String titleKey, Comp<?> content, LabelGraphic graphic) {
        return of(titleKey != null ? AppI18n.observable(titleKey) : null, content, graphic);
    }

    public static ModalOverlay of(ObservableValue<String> title, Comp<?> content, LabelGraphic graphic) {
        return new ModalOverlay(title, content, graphic, new ArrayList<>(), true, false, null);
    }

    public ModalOverlay withDefaultButtons(Runnable action) {
        addButton(ModalButton.cancel());
        addButton(ModalButton.ok(action));
        return this;
    }

    public ModalButton addButton(ModalButton button) {
        buttons.add(button);
        return button;
    }

    public void hideable(AppLayoutModel.QueueEntry entry) {
        setHideAction(() -> {
            AppLayoutModel.get().getQueueEntries().add(entry);
        });
    }

    public void addButtonBarComp(Comp<?> comp) {
        buttons.add(comp);
    }

    public void persist() {
        this.hasCloseButton = false;
        this.requireCloseButtonForClose = true;
    }

    public void show() {
        AppDialog.show(this, false);
    }

    public void hide() {
        AppDialog.hide(this);
    }

    public boolean isShowing() {
        return AppDialog.getModalOverlays().contains(this);
    }

    public void showAndWait() {
        AppDialog.showAndWait(this);
    }

    public void close() {
        AppDialog.closeDialog(this);
    }
}
