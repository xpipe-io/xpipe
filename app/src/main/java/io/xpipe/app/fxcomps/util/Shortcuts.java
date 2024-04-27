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

    private static final Map<Region, KeyCombination> DISPLAY_SHORTCUTS = new HashMap<>();

    public static void addDisplayShortcut(Region region, KeyCombination comb) {
        DISPLAY_SHORTCUTS.put(region, comb);
    }

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

        DISPLAY_SHORTCUTS.put(region, comb);
        AtomicReference<Scene> scene = new AtomicReference<>();
        region.sceneProperty().subscribe(s -> {
            if (Objects.equals(s, scene.get())) {
                return;
            }

            if (scene.get() != null) {
                scene.get().removeEventHandler(KeyEvent.KEY_PRESSED, filter);
                DISPLAY_SHORTCUTS.remove(region);
                scene.set(null);
            }

            if (s != null) {
                scene.set(s);
                DISPLAY_SHORTCUTS.put(region, comb);
                s.addEventHandler(KeyEvent.KEY_PRESSED, filter);
            }
        });
    }

    public static KeyCombination getDisplayShortcut(Region region) {
        return DISPLAY_SHORTCUTS.get(region);
    }
}
