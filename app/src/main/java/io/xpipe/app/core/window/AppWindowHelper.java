package io.xpipe.app.core.window;

import io.xpipe.app.core.*;
import io.xpipe.app.core.AppImages;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.InputHelper;
import io.xpipe.app.util.PlatformInit;
import io.xpipe.core.process.OsType;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import lombok.SneakyThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AppWindowHelper {

    public static Region alertContentText(String s) {
        return alertContentText(s, 450);
    }

    public static Region alertContentText(String s, int width) {
        var text = new Text(s);
        text.setWrappingWidth(width);
        var sp = new StackPane(text);
        sp.setPadding(new Insets(5));
        return sp;
    }

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

    public static void addIcons(Stage stage) {
        stage.getIcons().clear();

        // This allows for assigning logos even if AppImages has not been initialized yet
        var dir = OsType.getLocal() == OsType.MACOS ? "img/logo/padded" : "img/logo/full";
        AppResources.with(AppResources.XPIPE_MODULE, dir, path -> {
            var size =
                    switch (OsType.getLocal()) {
                        case OsType.Linux linux -> 128;
                        case OsType.MacOs macOs -> 128;
                        case OsType.Windows windows -> 32;
                    };
            stage.getIcons().add(AppImages.loadImage(path.resolve("logo_" + size + "x" + size + ".png")));
        });
    }

    public static void setContent(Alert alert, String s) {
        alert.getDialogPane().setMinWidth(505);
        alert.getDialogPane().setPrefWidth(505);
        alert.getDialogPane().setMaxWidth(505);
        alert.getDialogPane().setContent(AppWindowHelper.alertContentText(s));
    }

    @SneakyThrows
    public static Optional<ButtonType> showBlockingAlert(Consumer<Alert> c) {
        PlatformInit.init(true);

        Supplier<Alert> supplier = () -> {
            Alert a = AppWindowHelper.createEmptyAlert();
            var s = (Stage) a.getDialogPane().getScene().getWindow();
            s.setOnShown(event -> {
                Platform.runLater(() -> {
                    AppWindowBounds.clampWindow(s).ifPresent(rectangle2D -> {
                        s.setX(rectangle2D.getMinX());
                        s.setY(rectangle2D.getMinY());
                        // Somehow we have to set max size as setting the normal size does not work?
                        s.setMaxWidth(rectangle2D.getWidth());
                        s.setMaxHeight(rectangle2D.getHeight());
                    });
                });
                event.consume();
            });
            AppWindowBounds.fixInvalidStagePosition(s);
            AppWindowHelper.addFontSize(s);
            a.getDialogPane().getScene().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                if (new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN).match(event)) {
                    s.close();
                    event.consume();
                    return;
                }

                if (event.getCode().equals(KeyCode.ESCAPE)) {
                    s.close();
                    event.consume();
                }
            });
            return a;
        };

        AtomicReference<Optional<ButtonType>> result = new AtomicReference<>();
        if (!Platform.isFxApplicationThread()) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    Alert a = supplier.get();
                    c.accept(a);
                    result.set(a.showAndWait());
                } catch (Throwable t) {
                    result.set(Optional.empty());
                } finally {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }
        } else {
            Alert a = supplier.get();
            c.accept(a);
            result.set(a.showAndWait());
        }
        return result.get();
    }

    public static Alert createEmptyAlert() {
        Alert alert = new Alert(Alert.AlertType.NONE);
        if (AppMainWindow.getInstance() != null) {
            alert.initOwner(AppMainWindow.getInstance().getStage());
        }
        alert.getDialogPane().getScene().setFill(Color.TRANSPARENT);
        var stage = (Stage) alert.getDialogPane().getScene().getWindow();
        ModifiedStage.prepareStage(stage);
        addIcons(stage);
        setupStylesheets(alert.getDialogPane().getScene());
        return alert;
    }

    public static void setupStylesheets(Scene scene) {
        AppStyle.addStylesheets(scene);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (AppProperties.get().isDeveloperMode() && event.getCode().equals(KeyCode.F3)) {
                AppStyle.reloadStylesheets(scene);
                TrackEvent.debug("Reloaded stylesheets");
                event.consume();
            }
        });
        TrackEvent.debug("Set stylesheet reload listener");

        InputHelper.onNavigationInput(scene, (kb) -> {
            var r = scene.getRoot();
            if (r != null) {
                // This property is broken on some systems
                var acc = Platform.isAccessibilityActive();
                r.pseudoClassStateChanged(PseudoClass.getPseudoClass("key-navigation"), kb);
                r.pseudoClassStateChanged(PseudoClass.getPseudoClass("normal-navigation"), !kb);
                r.pseudoClassStateChanged(PseudoClass.getPseudoClass("accessibility-navigation"), acc);
            }
        });
    }

    public static void setupClickShield(Stage stage) {
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
