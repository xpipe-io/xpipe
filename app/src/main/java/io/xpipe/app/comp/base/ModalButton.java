package io.xpipe.app.comp.base;

import io.xpipe.app.core.mode.OperationMode;
import javafx.beans.property.Property;
import javafx.scene.control.Button;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.function.Consumer;

@Value
public class ModalButton {
    String key;
    Runnable action;
    boolean close;
    boolean defaultButton;

    public ModalButton(String key, Runnable action, boolean close, boolean defaultButton) {
        this.key = key;
        this.action = action;
        this.close = close;
        this.defaultButton = defaultButton;
    }

    @NonFinal
    Consumer<Button> augment;

    public static ModalButton finish(Runnable action) {
        return new ModalButton("finish", action, true, true);
    }

    public static ModalButton ok(Runnable action) {
        return new ModalButton("ok", action, true, true);
    }

    public static ModalButton ok() {
        return new ModalButton("ok", null, true, true);
    }

    public static ModalButton cancel() {
        return new ModalButton("cancel", null, true, false);
    }

    public static ModalButton skip() {
        return new ModalButton("skip", null, true, false);
    }

    public static ModalButton confirm(Runnable action) {
        return new ModalButton("confirm", action, true, true);
    }

    public static ModalButton quit() {
        return new ModalButton("quit", () -> {OperationMode.halt(1);}, true, false);
    }

    public ModalButton augment(Consumer<Button> augment) {
        this.augment = augment;
        return this;
    }

    public static Runnable toggle(Property<Boolean> prop) {
        return () -> {
            prop.setValue(true);
        };
    }
}
