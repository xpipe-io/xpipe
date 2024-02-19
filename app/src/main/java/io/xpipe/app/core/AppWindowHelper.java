package io.xpipe.app.core;

import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.OsType;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Arrays;
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
        if (AppMainWindow.getInstance() != null) {
            stage.initOwner(AppMainWindow.getInstance().getStage());
        }
        stage.setTitle(title);
        if (AppMainWindow.getInstance() != null) {
            stage.initOwner(AppMainWindow.getInstance().getStage());
        }

        addIcons(stage);
        setupContent(stage, contentFunc, bindSize, loading);
        setupStylesheets(stage.getScene());

        if (AppPrefs.get() != null && AppPrefs.get().enforceWindowModality().get()) {
            stage.initModality(Modality.WINDOW_MODAL);
        }

        stage.setOnShown(e -> {
            // If we set the theme pseudo classes earlier when the window is not shown
            // they do not apply. Is this a bug in JavaFX?
            Platform.runLater(() -> {
                AppTheme.initThemeHandlers(stage);
            });

            centerToMainWindow(stage);
            clampWindow(stage).ifPresent(rectangle2D -> {
                stage.setX(rectangle2D.getMinX());
                stage.setY(rectangle2D.getMinY());
                stage.setWidth(rectangle2D.getWidth());
                stage.setHeight(rectangle2D.getHeight());
            });
        });
        return stage;
    }

    private static void centerToMainWindow(Window childStage) {
        if (App.getApp() == null) {
            return;
        }

        var stage = App.getApp().getStage();
        childStage.setX(stage.getX() + stage.getWidth() / 2 - childStage.getWidth() / 2);
        childStage.setY(stage.getY() + stage.getHeight() / 2 - childStage.getHeight() / 2);
    }

    public static void showAlert(
            Consumer<Alert> c, Consumer<Optional<ButtonType>> bt) {
        ThreadHelper.runAsync(() -> {
            var r = showBlockingAlert(c);
            if (bt != null) {
                bt.accept(r);
            }
        });
    }

    public static void setContent(Alert alert, String s) {
        alert.getDialogPane().setMinWidth(505);
        alert.getDialogPane().setPrefWidth(505);
        alert.getDialogPane().setMaxWidth(505);
        alert.getDialogPane().setContent(AppWindowHelper.alertContentText(s));
    }

    public static boolean showConfirmationAlert(String title, String header, String content) {
        return AppWindowHelper.showBlockingAlert(alert -> {
                    alert.titleProperty().bind(AppI18n.observable(title));
                    alert.headerTextProperty().bind(AppI18n.observable(header));
                    setContent(alert, AppI18n.get(content));
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                })
                .map(b -> b.getButtonData().isDefaultButton())
                .orElse(false);
    }

    public static boolean showConfirmationAlert(ObservableValue<String> title, ObservableValue<String> header, ObservableValue<String> content) {
        return AppWindowHelper.showBlockingAlert(alert -> {
                    alert.titleProperty().bind(title);
                    alert.headerTextProperty().bind(header);
                    setContent(alert, content.getValue());
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                })
                .map(b -> b.getButtonData().isDefaultButton())
                .orElse(false);
    }

    public static Optional<ButtonType> showBlockingAlert(Consumer<Alert> c) {
        Supplier<Alert> supplier = () -> {
            Alert a = AppWindowHelper.createEmptyAlert();
            AppFont.normal(a.getDialogPane());
            var s = (Stage) a.getDialogPane().getScene().getWindow();
            s.setOnShown(event -> {
                clampWindow(s).ifPresent(rectangle2D -> {
                    s.setX(rectangle2D.getMinX());
                    s.setY(rectangle2D.getMinY());
                    // Somehow we have to set max size as setting the normal size does not work?
                    s.setMaxWidth(rectangle2D.getWidth());
                    s.setMaxHeight(rectangle2D.getHeight());
                });
                event.consume();
            });
            a.getDialogPane().getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (OsType.getLocal().equals(OsType.LINUX) || OsType.getLocal().equals(OsType.MACOS)) {
                    if (event.getCode().equals(KeyCode.W) && event.isShortcutDown()) {
                        s.close();
                        event.consume();
                        return;
                    }
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

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (OsType.getLocal().equals(OsType.LINUX) || OsType.getLocal().equals(OsType.MACOS)) {
                if (event.getCode().equals(KeyCode.W) && event.isShortcutDown()) {
                    stage.close();
                    event.consume();
                }
            }
        });
    }

    private static Optional<Rectangle2D> clampWindow(Stage stage) {
        if (!areNumbersValid(stage.getWidth(), stage.getHeight())) {
            return Optional.empty();
        }

        var allScreenBounds = computeWindowScreenBounds(stage);
        if (!areNumbersValid(allScreenBounds.getMinX(), allScreenBounds.getMinY(), allScreenBounds.getMaxX(), allScreenBounds.getMaxY())) {
            return Optional.empty();
        }

        // Alerts do not have a custom x/y set, but we are able to handle that

        boolean changed = false;

        double x = 0;
        if (areNumbersValid(stage.getX())) {
            x = stage.getX();
            if (x < allScreenBounds.getMinX()) {
                x = allScreenBounds.getMinX();
                changed = true;
            }
        }

        double y = 0;
        if (areNumbersValid(stage.getY())) {
            y = stage.getY();
            if (y < allScreenBounds.getMinY()) {
                y = allScreenBounds.getMinY();
                changed = true;
            }
        }

        double w = stage.getWidth();
        double h = stage.getHeight();
        if (x + w > allScreenBounds.getMaxX()) {
            w = allScreenBounds.getMaxX() - x;
            changed = true;
        }
        if (y + h > allScreenBounds.getMaxY()) {
            h = allScreenBounds.getMaxY() - y;
            changed = true;
        }

        // This should not happen but on weird Linux systems nothing is impossible
        if (w < 0 || h < 0) {
            return Optional.empty();
        }

        return changed ? Optional.of(new Rectangle2D(x, y, w, h)) : Optional.empty();
    }

    private static boolean areNumbersValid(double... args) {
        return Arrays.stream(args).allMatch(Double::isFinite);
    }

    private static List<Screen> getWindowScreens(Stage stage) {
        if (!areNumbersValid(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())) {
            return stage.getOwner() != null && stage.getOwner() instanceof Stage ownerStage ? getWindowScreens(ownerStage) : List.of(Screen.getPrimary());
        }

        return Screen.getScreensForRectangle(new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()));
    }

    private static Rectangle2D computeWindowScreenBounds(Stage stage) {
        double minX = Double.POSITIVE_INFINITY ;
        double minY = Double.POSITIVE_INFINITY ;
        double maxX = Double.NEGATIVE_INFINITY ;
        double maxY = Double.NEGATIVE_INFINITY ;
        for (Screen screen : getWindowScreens(stage)) {
            Rectangle2D screenBounds = screen.getBounds();
            if (screenBounds.getMinX() < minX) {
                minX = screenBounds.getMinX();
            }
            if (screenBounds.getMinY() < minY) {
                minY = screenBounds.getMinY() ;
            }
            if (screenBounds.getMaxX() > maxX) {
                maxX = screenBounds.getMaxX();
            }
            if (screenBounds.getMaxY() > maxY) {
                maxY = screenBounds.getMaxY() ;
            }
        }
        // Taskbar adjustment
        maxY -= 50;

        var w = maxX-minX;
        var h = maxY-minY;

        // This should not happen but on weird Linux systems nothing is impossible
        if (w < 0 || h < 0) {
            return new Rectangle2D(0,0,800, 600);
        }
        
        return new Rectangle2D(minX, minY, w, h);
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
