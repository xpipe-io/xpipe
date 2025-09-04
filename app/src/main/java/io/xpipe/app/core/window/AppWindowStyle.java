package io.xpipe.app.core.window;

import io.xpipe.app.core.*;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.OsType;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.scene.Scene;
import javafx.scene.input.*;
import javafx.stage.Stage;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

public class AppWindowStyle {

    public static void addMaximizedPseudoClass(Stage stage) {
        stage.getScene().rootProperty().subscribe(root -> {
            stage.maximizedProperty().subscribe(v -> {
                root.pseudoClassStateChanged(PseudoClass.getPseudoClass("maximized"), v);
            });
        });
    }

    public static void addFontSize(Stage stage) {
        stage.getScene().rootProperty().subscribe(root -> {
            AppFontSizes.base(root);
        });
    }

    public static void addNavigationStyleClasses(Scene scene) {
        Consumer<Boolean> onInput = kb -> {
            var r = scene.getRoot();
            if (r != null) {
                // This property is broken on some systems
                var acc = Platform.isAccessibilityActive();
                r.pseudoClassStateChanged(PseudoClass.getPseudoClass("key-navigation"), kb);
                r.pseudoClassStateChanged(PseudoClass.getPseudoClass("normal-navigation"), !kb);
                r.pseudoClassStateChanged(PseudoClass.getPseudoClass("accessibility-navigation"), acc);
            }
        };

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            var c = event.getCode();
            var list = List.of(KeyCode.SPACE, KeyCode.ENTER, KeyCode.SHIFT, KeyCode.TAB);
            onInput.accept(list.stream().anyMatch(keyCode -> keyCode == c)
                    || event.getCode().isNavigationKey());
        });
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            onInput.accept(false);
        });
    }

    public static void addIcons(Stage stage) {
        stage.getIcons().clear();

        // This allows for assigning logos even if AppImages has not been initialized yet
        var dir = OsType.getLocal() == OsType.MACOS ? "img/logo/padded" : "img/logo/full";
        AppResources.with(AppResources.MAIN_MODULE, dir, path -> {
            var size =
                    switch (OsType.getLocal()) {
                        case OsType.Linux ignored -> 128;
                        case OsType.MacOs ignored -> 128;
                        case OsType.Windows ignored -> 32;
                    };
            stage.getIcons().add(AppImages.loadImage(path.resolve("logo_" + size + "x" + size + ".png")));
        });
    }

    public static void addStylesheets(Scene scene) {
        AppStyle.addStylesheets(scene);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (AppProperties.get().isDevelopmentEnvironment()
                    && event.getCode().equals(KeyCode.F3)) {
                AppStyle.reloadStylesheets(scene);
                TrackEvent.debug("Reloaded stylesheets");
                event.consume();
            }
        });
        TrackEvent.debug("Set stylesheet reload listener");
    }

    public static void addClickShield(Stage stage) {
        if (OsType.getLocal() != OsType.MACOS) {
            return;
        }

        var focusInstant = new SimpleObjectProperty<>(Instant.EPOCH);
        stage.focusedProperty().subscribe((newValue) -> {
            if (newValue) {
                focusInstant.set(Instant.now());
            }
        });
        var blockNextPress = new SimpleBooleanProperty();
        stage.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            var elapsed = Duration.between(focusInstant.get(), Instant.now());
            if (elapsed.toMillis() < 50) {
                blockNextPress.set(true);
                event.consume();
            } else {
                blockNextPress.set(false);
            }
        });
        stage.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            var elapsed = Duration.between(focusInstant.get(), Instant.now());
            if (elapsed.toMillis() < 100 && blockNextPress.get()) {
                event.consume();
            }
        });
        stage.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            var elapsed = Duration.between(focusInstant.get(), Instant.now());
            if (elapsed.toMillis() < 1000 && blockNextPress.get()) {
                blockNextPress.set(false);
                event.consume();
            }
        });
        stage.addEventFilter(MouseEvent.ANY, event -> {
            if (!stage.isFocused()) {
                event.consume();
            }
        });
    }
}
