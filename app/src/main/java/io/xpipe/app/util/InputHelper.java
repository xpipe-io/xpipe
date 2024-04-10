package io.xpipe.app.util;

import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import java.util.List;
import java.util.function.Consumer;

public class InputHelper {

    public static void onLeft(EventTarget target, boolean filter, Consumer<KeyEvent> r) {
        EventHandler<KeyEvent> keyEventEventHandler = event -> {
            if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.NUMPAD4) {
                r.accept(event);
            }
        };
        if (filter) {
            target.addEventFilter(KeyEvent.KEY_PRESSED, keyEventEventHandler);
        } else {
            target.addEventHandler(KeyEvent.KEY_PRESSED, keyEventEventHandler);
        }
    }

    public static void onRight(EventTarget target, boolean filter, Consumer<KeyEvent> r) {
        EventHandler<KeyEvent> keyEventEventHandler = event -> {
            if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.NUMPAD6) {
                r.accept(event);
            }
        };
        if (filter) {
            target.addEventFilter(KeyEvent.KEY_PRESSED, keyEventEventHandler);
        } else {
            target.addEventHandler(KeyEvent.KEY_PRESSED, keyEventEventHandler);
        }
    }

    public static void onNavigationInput(EventTarget target, Consumer<Boolean> r) {
        target.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            var c = event.getCode();
            var list = List.of(
                    KeyCode.LEFT,
                    KeyCode.RIGHT,
                    KeyCode.UP,
                    KeyCode.DOWN,
                    KeyCode.SPACE,
                    KeyCode.ENTER,
                    KeyCode.SHIFT,
                    KeyCode.TAB,
                    KeyCode.NUMPAD2,
                    KeyCode.NUMPAD4,
                    KeyCode.NUMPAD6,
                    KeyCode.NUMPAD8);
            r.accept(list.stream().anyMatch(keyCode -> keyCode == c));
        });
        target.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            r.accept(false);
        });
    }
}
