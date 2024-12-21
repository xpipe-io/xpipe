package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.With;
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

    public static ModalOverlay of(String titleKey, Comp<?> content, Comp<?> graphic) {
        return new ModalOverlay(titleKey,content,graphic, new ArrayList<>(), false);
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
    Comp<?> graphic;

    @Singular
    List<ModalButton> buttons;

    @NonFinal
    boolean persistent;

    public ModalButton addButton(ModalButton button) {
        buttons.add(button);
        return button;
    }

    public void persist() {
        persistent = true;
    }
}
