package io.xpipe.app.comp.base;

import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.mode.AppOperationMode;

import io.xpipe.app.platform.PlatformThread;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;

import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Value
public class ModalButton {
    String key;
    Runnable action;
    boolean close;
    boolean defaultButton;
    List<Consumer<Button>> augments = new ArrayList<>();

    public ModalButton(String key, Runnable action, boolean close, boolean defaultButton) {
        this.key = key;
        this.action = action;
        this.close = close;
        this.defaultButton = defaultButton;
    }

    public static ModalButton ok(Runnable action) {
        return new ModalButton("ok", action, true, true);
    }

    public static ModalButton ok() {
        return new ModalButton("ok", null, true, true);
    }

    public static ModalButton done() {
        return new ModalButton("done", null, true, true);
    }

    public static ModalButton cancel() {
        return cancel(null);
    }

    public static ModalButton cancel(Runnable action) {
        return new ModalButton("cancel", action, true, false);
    }

    public static ModalButton confirm(Runnable action) {
        return new ModalButton("confirm", action, true, true);
    }

    public static ModalButton quit() {
        return new ModalButton(
                "quit",
                () -> {
                    AppOperationMode.halt(1);
                },
                true,
                false);
    }

    public static Runnable toggle(Property<Boolean> prop) {
        return () -> {
            prop.setValue(true);
        };
    }

    public ModalButton augment(Consumer<Button> augment) {
        this.augments.add(augment);
        return this;
    }

    public ModalButton loadingIndicator(ObservableValue<Boolean> busy) {
        return augment(button -> {
            button.graphicProperty()
                    .bind(Bindings.createObjectBinding(
                            () -> {
                                return busy.getValue()
                                        ? new LoadingIconComp(busy, AppFontSizes::base)
                                          .style("busy-loading-icon")
                                          .build()
                                        : null;
                            },
                            PlatformThread.sync(busy)));
            button.textProperty()
                    .bind(Bindings.createStringBinding(
                            () -> {
                                return !busy.getValue() ? AppI18n.get(key) : null;
                            },
                            PlatformThread.sync(busy),
                            AppI18n.activeLanguage()));
        });
    }

    public ModalButton disable(ObservableValue<Boolean> b) {
        return augment(button -> {
            button.disableProperty().bind(PlatformThread.sync(b));
        });
    }
}
