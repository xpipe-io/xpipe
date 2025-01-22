package io.xpipe.app.core.window;

import io.xpipe.app.comp.base.AppLayoutComp;
import io.xpipe.app.comp.base.AppMainWindowContentComp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.*;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.CloseBehaviourDialog;
import io.xpipe.app.resources.AppImages;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.OsType;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableValue;
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
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.imageio.ImageIO;

public class AppMainWindow {

    private static AppMainWindow INSTANCE;

    @Getter
    private final Stage stage;

    private final BooleanProperty windowActive = new SimpleBooleanProperty(false);
    private Thread thread;
    private volatile Instant lastUpdate;

    @Getter
    private static final Property<AppLayoutComp.Structure> loadedContent = new SimpleObjectProperty<>();

    @Getter
    private static final Property<String> loadingText = new SimpleObjectProperty<>();

    public AppMainWindow(Stage stage) {
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
        stage.setMinWidth(600);
        stage.setMinHeight(500);
        INSTANCE = new AppMainWindow(stage);

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

        ModifiedStage.prepareStage(stage);
        stage.setScene(scene);
        if (AppPrefs.get() != null) {
            stage.opacityProperty().bind(PlatformThread.sync(AppPrefs.get().windowOpacity()));
        }
        stage.titleProperty().bind(createTitle());
        AppWindowHelper.addIcons(stage);
        AppWindowHelper.setupStylesheets(stage.getScene());
        AppWindowHelper.setupClickShield(stage);
        AppWindowHelper.addMaximizedPseudoClass(stage);
        AppTheme.initThemeHandlers(stage);

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

    public ObservableDoubleValue displayScale() {
        if (getStage() == null) {
            return new SimpleDoubleProperty(1.0);
        }

        return getStage().outputScaleXProperty();
    }

    public static synchronized void initContent() {
        var content = new AppLayoutComp();
        var s = content.createStructure();
        loadedContent.setValue(s);
    }

    private static ObservableValue<String> createTitle() {
        if (AppPrefs.get() == null || LicenseProvider.get() == null) {
            return new SimpleStringProperty("XPipe");
        }

        var t = LicenseProvider.get().licenseTitle();
        var u = XPipeDistributionType.get().getUpdateHandler().getPreparedUpdate();
        return PlatformThread.sync(Bindings.createStringBinding(
                () -> {
                    var base = String.format(
                            "XPipe %s (%s)", t.getValue(), AppProperties.get().getVersion());
                    var prefix = AppProperties.get().isStaging() ? "[Public Test Build, Not a proper release] " : "";
                    var suffix = u.getValue() != null
                            ? " " + AppI18n.get("updateReadyTitle", u.getValue().getVersion())
                            : "";
                    return prefix + base + suffix;
                },
                u,
                t,
                AppI18n.activeLanguage()));
    }

    public void show() {
        stage.show();
        if (OsType.getLocal() == OsType.WINDOWS) {
            NativeWinWindowControl.MAIN_WINDOW = new NativeWinWindowControl(stage);
        }
    }

    public static AppMainWindow getInstance() {
        return INSTANCE;
    }

    private synchronized void onChange() {
        lastUpdate = Instant.now();
        if (thread == null) {
            thread = ThreadHelper.unstarted(() -> {
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

        stage.setOnShown(event -> {
            stage.requestFocus();
        });

        stage.setOnCloseRequest(e -> {
            if (!OperationMode.isInStartup() && !OperationMode.isInShutdown() && !CloseBehaviourDialog.showIfNeeded()) {
                e.consume();
                return;
            }

            // Close dialogs
            AppDialog.getModalOverlay().clear();

            // Close other windows
            Stage.getWindows().stream().filter(w -> !w.equals(stage)).toList().forEach(w -> w.fireEvent(e));

            // Close self
            stage.close();
            OperationMode.onWindowClose();
            e.consume();
        });

        stage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN).match(event)) {
                stage.close();
                OperationMode.onWindowClose();
                event.consume();
            }
        });

        if (OsType.getLocal().equals(OsType.LINUX) || OsType.getLocal().equals(OsType.MACOS)) {
            stage.getScene().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                if (new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN).match(event)) {
                    OperationMode.onWindowClose();
                    event.consume();
                }
            });
        }

        stage.getScene().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (AppProperties.get().isShowcase() && event.getCode().equals(KeyCode.F12)) {
                var image = stage.getScene().snapshot(null);
                var awt = AppImages.toAwtImage(image);
                var file = Path.of(System.getProperty("user.home"), "Desktop", "xpipe-screenshot.png");
                try {
                    ImageIO.write(awt, "png", file.toFile());
                } catch (IOException e) {
                    ErrorEvent.fromThrowable(e).handle();
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
                stage.setHeight(720);
            } else {
                stage.setX(state.windowX);
                stage.setY(state.windowY);
                stage.setWidth(state.windowWidth);
                stage.setHeight(state.windowHeight);
            }
            TrackEvent.debug("Window loaded saved bounds");
        } else if (!AppProperties.get().isShowcase()) {
            stage.setWidth(1280);
            stage.setHeight(720);
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
