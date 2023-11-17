package io.xpipe.app.core;

import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class AppWindowHelper {

    public static Node alertContentText(String s) {
        var text = new Text(s);
        text.setWrappingWidth(450);
        AppFont.medium(text);
        var sp = new StackPane(text);
        sp.setPadding(new Insets(5));
        return sp;
    }

    public static void addIcons(Stage stage) {
        stage.getIcons().clear();

        // This allows for assigning logos even if AppImages has not been initialized yet
        AppResources.with(AppResources.XPIPE_MODULE, "img/logo", path -> {
            for (String s : List.of(
                    "logo_16x16.png",
                    "logo_24x24.png",
                    "logo_32x32.png",
                    "logo_48x48.png",
                    "logo_128x128.png",
                    "logo_256x256.png")) {
                stage.getIcons().add(AppImages.loadImage(path.resolve(s)));
            }
        });
    }

    public static Stage sideWindow(
            String title, Function<Stage, Comp<?>> contentFunc, boolean bindSize, ObservableValue<Boolean> loading) {
        var stage = new Stage();
        stage.setTitle(title);
        addIcons(stage);
        setupContent(stage, contentFunc, bindSize, loading);
        AppTheme.initThemeHandlers(stage);
        setupStylesheets(stage.getScene());

        stage.setOnShown(e -> {
            centerToMainWindow(stage);
        });
        return stage;
    }

    private static void centerToMainWindow(Stage childStage) {
        if (App.getApp() == null) {
            return;
        }

        var stage = App.getApp().getStage();
        childStage.setX(stage.getX() + stage.getWidth() / 2 - childStage.getWidth() / 2);
        childStage.setY(stage.getY() + stage.getHeight() / 2 - childStage.getHeight() / 2);
    }

    public static void showAlert(
            Consumer<Alert> c, ObservableValue<Boolean> loading, Consumer<Optional<ButtonType>> bt) {
        ThreadHelper.runAsync(() -> {
            var r = showBlockingAlert(c);
            if (bt != null) {
                bt.accept(r);
            }
        });
    }

    public static void showAndWaitForWindow(Supplier<Stage> s) {
        if (!Platform.isFxApplicationThread()) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                s.get().showAndWait();
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }
        } else {
            s.get().showAndWait();
        }
    }

    public static Optional<ButtonType> showBlockingAlert(Consumer<Alert> c) {
        AtomicReference<Optional<ButtonType>> result = new AtomicReference<>();
        if (!Platform.isFxApplicationThread()) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                Alert a = AppWindowHelper.createEmptyAlert();
                AppFont.normal(a.getDialogPane());

                c.accept(a);
                result.set(a.showAndWait());
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }
        } else {
            Alert a = createEmptyAlert();
            AppFont.normal(a.getDialogPane());
            c.accept(a);

            Button button = (Button) a.getDialogPane().lookupButton(ButtonType.OK);
            if (button != null) {
                button.getStyleClass().add("ok-button");
            }

            result.set(a.showAndWait());
        }
        return result.get();
    }

    public static Alert createEmptyAlert() {
        Alert alert = new Alert(Alert.AlertType.NONE);
        addIcons(((Stage) alert.getDialogPane().getScene().getWindow()));
        setupStylesheets(alert.getDialogPane().getScene());
        return alert;
    }

    public static void setupStylesheets(Scene scene) {
        AppStyle.addStylesheets(scene);

        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (AppProperties.get().isDeveloperMode() && event.getCode().equals(KeyCode.F3)) {
                AppStyle.reloadStylesheets(scene);
                TrackEvent.debug("Reloaded stylesheets");
                event.consume();
            }
        });
        TrackEvent.debug("Set stylesheet reload listener");
    }

    public static void setupContent(
            Stage stage, Function<Stage, Comp<?>> contentFunc, boolean bindSize, ObservableValue<Boolean> loading) {
        var baseComp = contentFunc.apply(stage);
        var content = loading != null ? LoadingOverlayComp.noProgress(baseComp, loading) : baseComp;
        var contentR = content.createRegion();
        AppFont.small(contentR);
        var scene = new Scene(bindSize ? new Pane(contentR) : contentR, -1, -1, false);
        stage.setScene(scene);
        contentR.requestFocus();
        if (bindSize) {
            bindSize(stage, contentR);
            stage.setResizable(false);
        }

        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (AppProperties.get().isDeveloperMode() && event.getCode().equals(KeyCode.F6)) {
                var newBaseComp = contentFunc.apply(stage);
                var newComp = loading != null ? LoadingOverlayComp.noProgress(newBaseComp, loading) : newBaseComp;
                var newR = newComp.createRegion();
                AppFont.medium(newR);
                scene.setRoot(bindSize ? new Pane(newR) : newR);
                newR.requestFocus();
                if (bindSize) {
                    bindSize(stage, newR);
                }

                TrackEvent.debug("Rebuilt content");
                event.consume();
            }
        });
    }

    private static void bindSize(Stage stage, Region r) {
        if (r.getPrefWidth() == Region.USE_COMPUTED_SIZE) {
            r.widthProperty().addListener((c, o, n) -> {
                stage.sizeToScene();
            });
        } else {
            stage.setWidth(r.getPrefWidth());
            r.prefWidthProperty().addListener((c, o, n) -> {
                stage.sizeToScene();
            });
        }

        if (r.getPrefHeight() == Region.USE_COMPUTED_SIZE) {
            r.heightProperty().addListener((c, o, n) -> {
                stage.sizeToScene();
            });
        } else {
            stage.setHeight(r.getPrefHeight());
            r.prefHeightProperty().addListener((c, o, n) -> {
                stage.sizeToScene();
            });
        }

        stage.sizeToScene();
    }
}
