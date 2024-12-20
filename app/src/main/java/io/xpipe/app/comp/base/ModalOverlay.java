package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.With;

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
        return new ModalOverlay(titleKey,content,graphic, new ArrayList<>());
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

    public void addButton(ModalButton button) {
        buttons.add(button);
    }

    @Value
    public static class ModalButton {
        String key;
        Runnable action;
        boolean close;
        boolean defaultButton;

        public static ModalButton finish(Runnable action) {
            return new ModalButton("finish", action, true, true);
        }

        public static ModalButton ok(Runnable action) {
            return new ModalButton("ok", action, true, true);
        }

        public static ModalButton cancel() {
            return new ModalButton("cancel", () -> {}, true, false);
        }
    }
}
