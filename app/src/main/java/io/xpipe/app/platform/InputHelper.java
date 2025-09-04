package io.xpipe.app.platform;

import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.scene.input.*;

import java.util.function.Consumer;

public class InputHelper {

    public static void onKeyCombination(EventTarget target, KeyCombination c, boolean filter, Consumer<KeyEvent> r) {
        EventHandler<KeyEvent> keyEventEventHandler = event -> {
            if (c.match(event)) {
                r.accept(event);
            }
        };
        if (filter) {
            target.addEventFilter(KeyEvent.KEY_PRESSED, keyEventEventHandler);
        } else {
            target.addEventHandler(KeyEvent.KEY_PRESSED, keyEventEventHandler);
        }
    }

    public static void onExactKeyCode(EventTarget target, KeyCode code, boolean filter, Consumer<KeyEvent> r) {
        EventHandler<KeyEvent> keyEventEventHandler = event -> {
            if (new KeyCodeCombination(code).match(event)) {
                r.accept(event);
            }
        };
        if (filter) {
            target.addEventFilter(KeyEvent.KEY_PRESSED, keyEventEventHandler);
        } else {
            target.addEventHandler(KeyEvent.KEY_PRESSED, keyEventEventHandler);
        }
    }

    public static void onLeft(EventTarget target, boolean filter, Consumer<KeyEvent> r) {
        EventHandler<KeyEvent> e = event -> {
            if (new KeyCodeCombination(KeyCode.LEFT).match(event)
                    || new KeyCodeCombination(KeyCode.NUMPAD4).match(event)) {
                r.accept(event);
            }
        };
        if (filter) {
            target.addEventFilter(KeyEvent.KEY_PRESSED, e);
        } else {
            target.addEventHandler(KeyEvent.KEY_PRESSED, e);
        }
    }

    public static void onRight(EventTarget target, boolean filter, Consumer<KeyEvent> r) {
        EventHandler<KeyEvent> e = event -> {
            if (new KeyCodeCombination(KeyCode.RIGHT).match(event)
                    || new KeyCodeCombination(KeyCode.NUMPAD6).match(event)) {
                r.accept(event);
            }
        };
        if (filter) {
            target.addEventFilter(KeyEvent.KEY_PRESSED, e);
        } else {
            target.addEventHandler(KeyEvent.KEY_PRESSED, e);
        }
    }
}
