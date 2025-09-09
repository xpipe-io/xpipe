package io.xpipe.app.core.window;

import io.xpipe.app.comp.base.AppLayoutComp;
import io.xpipe.app.comp.base.AppMainWindowContentComp;
import io.xpipe.app.core.*;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.platform.NativeWinWindowControl;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.CloseBehaviourDialog;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.core.OsType;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableDoubleValue;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import javax.imageio.ImageIO;

public class AppMainWindow {

    @Getter
    private static final Property<AppLayoutComp.Structure> loadedContent = new SimpleObjectProperty<>();

    @Getter
    private static final Property<String> loadingText = new SimpleObjectProperty<>();

    private static AppMainWindow INSTANCE;

    @Getter
    private final Stage stage;

    private final BooleanProperty windowActive = new SimpleBooleanProperty(false);
    private volatile Instant lastUpdate;
    private boolean shown = false;

    private AppMainWindow(Stage stage) {
        this.stage = stage;
    }

    public static void init(boolean show) {
        if (INSTANCE != null
                && INSTANCE.getStage() != null
                && (!show || INSTANCE.getStage().isShowing())) {
            return;
        }

        PlatformThread.runLaterIfNeededBlocking(() -> {
            initEmpty(show);
        });
    }

    private static synchronized void initEmpty(boolean show) {
        if (INSTANCE != null) {
            if (show) {
                INSTANCE.show();
            }
            return;
        }

        var stage = App.getApp().getStage();
        stage.setMinWidth(500);
        stage.setMinHeight(400);
        INSTANCE = new AppMainWindow(stage);
        AppModifiedStage.prepareStage(stage);

        var content = new AppMainWindowContentComp(stage).createRegion();
        content.opacityProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> {
                            if (OsType.getLocal() != OsType.MACOS) {
                                return 1.0;
                            }
                            return stage.isFocused() ? 1.0 : 0.8;
                        },
                        stage.focusedProperty()));
        var scene = new Scene(content, -1, -1, false);
        content.prefWidthProperty().bind(scene.widthProperty());
        content.prefHeightProperty().bind(scene.heightProperty());
        scene.setFill(Color.TRANSPARENT);

        stage.setScene(scene);
        if (AppPrefs.get() != null) {
            stage.opacityProperty().bind(PlatformThread.sync(AppPrefs.get().windowOpacity()));
        }
        AppWindowStyle.addIcons(stage);
        AppWindowStyle.addStylesheets(stage.getScene());
        AppWindowStyle.addNavigationPseudoClasses(stage.getScene());
        AppWindowStyle.addClickShield(stage);
        AppWindowStyle.addMaximizedPseudoClass(stage);
        AppWindowStyle.addFontSize(stage);
        AppTheme.initThemeHandlers(stage);

        AppWindowTitle.getTitle().subscribe(s -> {
            PlatformThread.runLaterIfNeeded(() -> {
                stage.setTitle(s);
            });
        });

        var state = INSTANCE.loadState();
        TrackEvent.withDebug("Window state loaded").tag("state", state).handle();
        INSTANCE.initializeWindow(state);
        INSTANCE.setupListeners();
        INSTANCE.windowActive.set(true);

        if (show) {
            INSTANCE.show();
        }
    }

    public static void loadingText(String key) {
        loadingText.setValue(key != null && AppI18n.get() != null ? AppI18n.get(key) : "...");
    }

    public static synchronized void initContent() {
        PlatformThread.runLaterIfNeededBlocking(() -> {
            try {
                TrackEvent.info("Window content node creation started");
                var content = new AppLayoutComp();
                var s = content.createStructure();
                TrackEvent.info("Window content node structure created");
                loadedContent.setValue(s);
            } catch (Throwable t) {
                ErrorEventFactory.fromThrowable(t).term().handle();
            }
        });
    }

    public static AppMainWindow get() {
        return INSTANCE;
    }

    public ObservableDoubleValue displayScale() {
        if (getStage() == null) {
            return new SimpleDoubleProperty(1.0);
        }

        return getStage().outputScaleXProperty();
    }

    public void show() {
        stage.show();
        if (OsType.getLocal() == OsType.WINDOWS && !shown) {
            var ctrl = new NativeWinWindowControl(stage);
            NativeWinWindowControl.MAIN_WINDOW = ctrl;
            AppWindowsShutdown.registerHook(ctrl.getWindowHandle());
        }
        shown = true;
    }

    public void focus() {
        if (AppPrefs.get() != null
                && !AppPrefs.get().focusWindowOnNotifications().get()) {
            return;
        }

        PlatformThread.runLaterIfNeeded(() -> {
            if (!stage.isShowing()) {
                return;
            }

            stage.setIconified(false);
            stage.requestFocus();
        });
    }

    private synchronized void onChange() {
        var timestamp = Instant.now();
        lastUpdate = timestamp;
        // Reduce printed window updates
        GlobalTimer.delay(
                () -> {
                    if (!timestamp.equals(lastUpdate)) {
                        return;
                    }

                    synchronized (AppMainWindow.this) {
                        logChange();
                    }
                },
                Duration.ofSeconds(1));
    }

    private void logChange() {
        TrackEvent.withDebug("Window resize")
                .tag("x", stage.getX())
                .tag("y", stage.getY())
                .tag("width", stage.getWidth())
                .tag("height", stage.getHeight())
                .tag("maximized", stage.isMaximized())
                .build()
                .handle();
    }

    private void initializeWindow(WindowState state) {
        applyState(state);

        TrackEvent.withDebug("Window initialized")
                .tag("x", stage.getX())
                .tag("y", stage.getY())
                .tag("width", stage.getWidth())
                .tag("height", stage.getHeight())
                .tag("maximized", stage.isMaximized())
                .build()
                .handle();
    }

    private void setupListeners() {
        AppWindowBounds.fixInvalidStagePosition(stage);
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
            if (!AppOperationMode.isInStartup()
                    && !AppOperationMode.isInShutdown()
                    && !CloseBehaviourDialog.showIfNeeded()) {
                e.consume();
                return;
            }

            // Close dialogs
            AppDialog.getModalOverlays().clear();

            // Close other windows
            Stage.getWindows().stream().filter(w -> !w.equals(stage)).toList().forEach(w -> w.fireEvent(e));

            // Iconifying stages on Windows will break if the window is closed
            // Work around this issue it by re-showing it immediately before hiding it again
            if (OsType.getLocal() == OsType.WINDOWS) {
                stage.setIconified(false);
            }

            // Close self
            stage.close();
            AppOperationMode.onWindowClose();
            e.consume();
        });

        stage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN).match(event)) {
                stage.close();
                AppOperationMode.onWindowClose();
                event.consume();
            }
        });

        if (OsType.getLocal() == OsType.LINUX || OsType.getLocal() == OsType.MACOS) {
            stage.getScene().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                if (new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN).match(event)) {
                    AppOperationMode.onWindowClose();
                    event.consume();
                }
            });
        }

        stage.getScene().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (AppProperties.get().isShowcase() && event.getCode().equals(KeyCode.F12)) {
                var image = stage.getScene().snapshot(null);
                var awt = AppImages.toAwtImage(image);
                var file = AppSystemInfo.ofCurrent()
                        .getUserHome()
                        .resolve("Desktop", AppNames.ofCurrent().getKebapName() + "-screenshot.png");
                try {
                    ImageIO.write(awt, "png", file.toFile());
                } catch (IOException e) {
                    ErrorEventFactory.fromThrowable(e).handle();
                }
                TrackEvent.debug("Screenshot taken");
                event.consume();
            }
        });

        TrackEvent.debug("Window listeners added");
    }

    private void applyState(WindowState state) {
        if (state != null) {
            if (state.maximized) {
                stage.setMaximized(true);
                stage.setWidth(1280);
                stage.setHeight(780);
            } else {
                stage.setX(state.windowX);
                stage.setY(state.windowY);
                stage.setWidth(state.windowWidth);
                stage.setHeight(state.windowHeight);
            }
            TrackEvent.debug("Window loaded saved bounds");
        } else if (!AppProperties.get().isShowcase()) {
            if (AppDistributionType.get() == AppDistributionType.WEBTOP) {
                stage.setWidth(1000);
                stage.setHeight(600);
            } else {
                stage.setWidth(1280);
                stage.setHeight(780);
            }
        } else {
            stage.setX(312);
            stage.setY(149);
            stage.setWidth(1296);
            stage.setHeight(759);
        }
    }

    private void saveState() {
        if (AppPrefs.get() == null || !AppPrefs.get().saveWindowLocation().get()) {
            return;
        }

        if (AppProperties.get().isShowcase()) {
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
        if (AppPrefs.get() == null) {
            return null;
        }

        if (!AppPrefs.get().saveWindowLocation().get()) {
            return null;
        }

        if (AppProperties.get().isShowcase()) {
            return null;
        }

        WindowState state = AppCache.getNonNull("windowState", WindowState.class, () -> null);
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
