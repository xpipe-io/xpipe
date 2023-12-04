package io.xpipe.app.fxcomps.util;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Shortcuts {

    private static final Map<Region, KeyCombination> SHORTCUTS = new HashMap<>();

    public static <T extends ButtonBase> void addShortcut(T region, KeyCombination comb) {
        addShortcut(region, comb, ButtonBase::fire);
    }

    public static <T extends Region> void addShortcut(T region, KeyCombination comb, Consumer<T> exec) {
        var filter = new EventHandler<KeyEvent>() {
            public void handle(KeyEvent ke) {
                if (!region.isVisible() || !region.isManaged() || region.isDisabled()) {
                    return;
                }

                if (comb.match(ke)) {
                    exec.accept(region);
                    ke.consume();
                }
            }
        };

        AtomicReference<Scene> scene = new AtomicReference<>();
        SHORTCUTS.put(region, comb);
        SimpleChangeListener.apply(region.sceneProperty(), s -> {
            if (Objects.equals(s, scene.get())) {
                return;
            }

            if (scene.get() != null) {
                scene.get().removeEventFilter(KeyEvent.KEY_PRESSED, filter);
                SHORTCUTS.remove(region);
                scene.set(null);
            }

            if (s != null) {
                scene.set(s);
                s.addEventFilter(KeyEvent.KEY_PRESSED, filter);
            }
        });
    }

    public static KeyCombination getShortcut(Region region) {
        return SHORTCUTS.get(region);
    }
}
