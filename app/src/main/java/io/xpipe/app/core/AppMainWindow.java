package io.xpipe.app.core;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.CloseBehaviourAlert;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AppMainWindow {

    private static AppMainWindow INSTANCE;

    @Getter
    private final Stage stage;

    private final BooleanProperty windowActive = new SimpleBooleanProperty(false);
    private Thread thread;
    private volatile Instant lastUpdate;

    public AppMainWindow(Stage stage) {
        this.stage = stage;
    }

    public static AppMainWindow init(Stage stage) {
        INSTANCE = new AppMainWindow(stage);
        var scene = new Scene(new Region(), -1, -1, false);
        stage.setScene(scene);
        AppWindowHelper.setupStylesheets(stage.getScene());
        return INSTANCE;
    }

    private synchronized void onChange() {
        lastUpdate = Instant.now();
        if (thread == null) {
            thread = ThreadHelper.create("window change timeout", true, () -> {
                while (true) {
                    var toStop = lastUpdate.plus(Duration.of(1, ChronoUnit.SECONDS));
                    if (Instant.now().isBefore(toStop)) {
                        var toSleep = Duration.between(Instant.now(), toStop);
                        if (!toSleep.isNegative()) {
                            var ms = toSleep.toMillis();
                            ThreadHelper.sleep(ms);
                        }
                    } else {
                        break;
                    }
                }

                synchronized (AppMainWindow.this) {
                    logChange();
                    thread = null;
                }
            });
            thread.start();
        }
    }

    private void logChange() {
        TrackEvent.withDebug("Window resize")
                .windowCategory()
                .tag("x", stage.getX())
                .tag("y", stage.getY())
                .tag("width", stage.getWidth())
                .tag("height", stage.getHeight())
                .tag("maximized", stage.isMaximized())
                .build()
                .handle();
    }

    private void initializeWindow() {
        var state = loadState();
        applyState(state);

        TrackEvent.withDebug("Window initialized")
                .windowCategory()
                .tag("x", stage.getX())
                .tag("y", stage.getY())
                .tag("width", stage.getWidth())
                .tag("height", stage.getHeight())
                .tag("maximized", stage.isMaximized())
                .build()
                .handle();
    }

    private void setupListeners() {
        stage.xProperty().addListener((c, o, n) -> {
            if (windowActive.get()) {
                onChange();
            }
        });
        stage.yProperty().addListener((c, o, n) -> {
            if (windowActive.get()) {
                onChange();
            }
        });
        stage.widthProperty().addListener((c, o, n) -> {
            if (windowActive.get()) {
                onChange();
            }
        });
        stage.heightProperty().addListener((c, o, n) -> {
            if (windowActive.get()) {
                onChange();
            }
        });
        stage.maximizedProperty().addListener((c, o, n) -> {
            if (windowActive.get()) {
                onChange();
            }
        });

        stage.setOnHiding(e -> {
            saveState();
        });

        stage.setOnHidden(e -> {
            windowActive.set(false);
        });

        stage.setOnCloseRequest(e -> {
            if (!CloseBehaviourAlert.showIfNeeded()) {
                e.consume();
                return;
            }

            // Close other windows
            Stage.getWindows().stream().filter(w -> !w.equals(stage)).toList().forEach(w -> w.fireEvent(e));
            stage.close();

            AppPrefs.get().closeBehaviour().getValue().getExit().run();
            e.consume();
        });

        TrackEvent.debug("Window listeners added");
    }

    private void applyState(WindowState state) {
        if (state != null) {
            stage.setX(state.windowX);
            stage.setY(state.windowY);
            stage.setWidth(state.windowWidth);
            stage.setHeight(state.windowHeight);
            // stage.setMaximized(state.maximized);

            TrackEvent.debug("Window loaded saved bounds");
        } else {
            stage.setWidth(1280);
            stage.setHeight(720);
        }
    }

    private void saveState() {
        if (!AppPrefs.get().saveWindowLocation.get()) {
            return;
        }

        var newState = WindowState.builder()
                .maximized(stage.isMaximized())
                .windowX((int) stage.getX())
                .windowY((int) stage.getY())
                .windowWidth((int) stage.getWidth())
                .windowHeight((int) stage.getHeight())
                .build();
        AppCache.update("windowState", newState);
    }

    private WindowState loadState() {
        if (!AppPrefs.get().saveWindowLocation.get()) {
            return null;
        }

        WindowState state = AppCache.get("windowState", WindowState.class, () -> null);
        if (state == null) {
            return null;
        }

        boolean inBounds = false;
        for (Screen screen : Screen.getScreens()) {
            Rectangle2D visualBounds = screen.getVisualBounds();
            // Check whether the bounds intersect where the intersection is larger than 20 pixels!
            if (state.windowWidth > 40
                    && state.windowHeight > 40
                    && visualBounds.intersects(new Rectangle2D(
                            state.windowX + 20, state.windowY + 20, state.windowWidth - 40, state.windowHeight - 40))) {
                inBounds = true;
                break;
            }
        }
        return inBounds ? state : null;
    }

    public void initialize() {
        initializeWindow();
        setupListeners();
        windowActive.set(true);
        TrackEvent.debug("Window set to active");
    }

    public void show() {
        stage.show();
    }

    private void setupContent(Comp<?> content) {
        var contentR = content.createRegion();
        contentR.requestFocus();
        stage.getScene().setRoot(contentR);
        TrackEvent.debug("Set content scene");

        stage.getScene().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (AppProperties.get().isDeveloperMode() && event.getCode().equals(KeyCode.F6)) {
                var newR = content.createRegion();
                stage.getScene().setRoot(newR);
                newR.requestFocus();

                TrackEvent.debug("Rebuilt content");
                event.consume();
            }
        });
        TrackEvent.debug("Set content reload listener");
    }

    public void setContent(Comp<?> content) {
        setupContent(content);
    }

    @Builder
    @Jacksonized
    @Value
    private static class WindowState {
        boolean maximized;
        int windowX;
        int windowY;
        int windowWidth;
        int windowHeight;
    }
}
