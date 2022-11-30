package io.xpipe.extension.fxcomps.util;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Shortcuts {

    private static final Map<Region, KeyCombination> SHORTCUTS = new HashMap<>();

    public static <T extends ButtonBase> void addShortcut(T region, KeyCombination comb) {
        addShortcut(region, comb, ButtonBase::fire);
    }

    public static <T extends Region> void addShortcut(T region, KeyCombination comb, Consumer<T> exec) {
        AtomicReference<Scene> scene = new AtomicReference<>(region.getScene());
        var filter = new EventHandler<KeyEvent>() {
            public void handle(KeyEvent ke) {
                if (comb.match(ke)) {
                    exec.accept(region);
                    ke.consume();
                }
            }
        };
        SHORTCUTS.put(region, comb);

        SimpleChangeListener.apply(region.sceneProperty(), s -> {
            if (s != null) {
                scene.set(s);
                s.addEventHandler(KeyEvent.KEY_PRESSED, filter);
                SHORTCUTS.put(region, comb);
            } else {
                if (scene.get() == null) {
                    return;
                }

                scene.get().removeEventHandler(KeyEvent.KEY_PRESSED, filter);
                SHORTCUTS.remove(region);
                scene.set(null);
            }
        });
    }

    public static KeyCombination getShortcut(Region region) {
        return SHORTCUTS.get(region);
    }
}
