package io.xpipe.app.util;

import io.xpipe.app.core.*;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.core.window.AppWindowStyle;
import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.OsType;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;

public class RemoteDesktopWindow {

    private RemoteDesktopWindow(State state, RemoteDesktopDockView model) {
        this.state = state;
        this.model = model;
    }

    public static void init() {
        State state = AppCache.getNonNull("remoteDesktopWindowState", State.class, () -> null);
        var model = new RemoteDesktopDockView(rect -> rect, () -> {
            return INSTANCE.nativeWinWindowControl;
        });
        INSTANCE = new RemoteDesktopWindow(state, model);
        INSTANCE.startStateListener();
    }

    public static void reset() {
        AppCache.update("remoteDesktopWindowState", INSTANCE.state);
        INSTANCE = null;
    }

    private State state;
    @Getter
    private Stage stage;
    private NativeWinWindowControl nativeWinWindowControl;

    @Getter
    private final RemoteDesktopDockView model;

    @Getter
    private final ObjectProperty<RemoteDesktopDockEntry> selected = new SimpleObjectProperty<>();

    @Getter
    private final ObservableList<RemoteDesktopDockEntry> processes = FXCollections.observableArrayList();

    @Getter
    private final BooleanProperty restartTriggered = new SimpleBooleanProperty();

    public boolean supportsDocking() {
        return OsType.ofLocal() == OsType.WINDOWS;
    }

    private void createStage() {
        if (stage != null) {
            return;
        }

        stage = new Stage();
        stage.initStyle(StageStyle.UNIFIED);
        var scene = new Scene(new Region());
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.getScene().setRoot(new RemoteDesktopDockComp().build());
        stage.setWidth(AppMainWindow.get() != null ? AppMainWindow.get().getStage().getWidth() : 1280);
        stage.setHeight(AppMainWindow.get() != null ? AppMainWindow.get().getStage().getHeight() : 780);
        stage.titleProperty().bind(PlatformThread.sync(createTitle()));

        // We close this automatically after all children are gone
        stage.setOnCloseRequest(event -> {
            // Allow to close stuck window if somehow it is still showing with no tabs
            if (processes.isEmpty()) {
                return;
            }

            AppCache.update("remoteDesktopWindowState", state);
            // The dock handles the closing of the tabs
            if (supportsDocking()) {
                event.consume();
            } else {
                model.onClose();
                event.consume();
            }
        });

        if (AppPrefs.get() != null) {
            stage.opacityProperty().bind(PlatformThread.sync(AppPrefs.get().windowOpacity()));
        }
        AppWindowStyle.addIcons(stage);
        AppWindowStyle.addStylesheets(stage.getScene());
        AppWindowStyle.addMaximizedPseudoClass(stage);
        AppWindowStyle.addFontSize(stage);
        AppTheme.initThemeHandlers(stage);

        setupWindowListeners();
    }

    private void applyStageState() {
        if (state != null) {
            if (state.maximized) {
                stage.setMaximized(true);
            } else {
                stage.setX(state.windowX);
                stage.setY(state.windowY);
                stage.setWidth(state.windowWidth);
                stage.setHeight(state.windowHeight);
                stage.setMaximized(false);
            }
        }
    }

    @SneakyThrows
    public void show() {
        PlatformThread.runLaterIfNeededBlocking(() -> {
            if (stage != null && stage.isShowing()) {
                stage.setIconified(false);
                return;
            }

            createStage();
            applyStageState();
            stage.show();

            if (supportsDocking()) {
                nativeWinWindowControl = new NativeWinWindowControl(stage);
                nativeWinWindowControl.setWindowsTransitionsEnabled(false);
            }
        });

        // Wait two more pulses for window to take correct size
        var latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            Platform.runLater(() -> {
                latch.countDown();
            });
        });
        latch.await();
    }

    public void focus() {
        PlatformThread.runLaterIfNeeded(() -> {
            model.focus();
        });
    }

    public void select(RemoteDesktopDockEntry entry) {
        model.select(entry);
        selected.set(entry);
    }

    public void close(RemoteDesktopDockEntry entry) {
        model.closeWindow(entry);
    }

    public void trackInternal(String name, String icon, DataStoreColor color, DataStoreEntry e, RemoteDesktopDockContentEntry entry) {
        var toTrack = new RemoteDesktopDockEntry(name, icon, color, e, null, entry, null, null);
        model.track(toTrack);
    }

    public void trackExternal(String name, String icon, DataStoreColor color, DataStoreEntry e, int w, int h, Process process, Duration maxWait, Predicate<ControllableWindowProcess> filter) {
        var start = process.info().startInstant().orElseThrow();
        GlobalTimer.scheduleUntil(Duration.ofMillis(200), false, () -> {
            if (Duration.between(start, Instant.now()).compareTo(maxWait) > 0) {
                return true;
            }

            if (!stage.isShowing()) {
                return true;
            }

            if (!process.isAlive()) {
                return true;
            }

//            var after = ProcessHandle.allProcesses().map(processHandle -> {
//                var spawnedAfter = processHandle.info().startInstant().map(instant -> instant.equals(start) || instant.isAfter(start)).orElse(false);
//                if (!spawnedAfter) {
//                    return null;
//                }
//
//                var windows = NativeWinWindowControl.byPid(processHandle.pid());
//                if (windows.isEmpty()) {
//                    return null;
//                }
//
//                var c = new ControllableWindowsProcess(processHandle, windows.getFirst());
//                if (!filter.test(c)) {
//                    return null;
//                }
//
//                return c;
//            }).filter(Objects::nonNull).findFirst();

            var windows = NativeWinWindowControl.byPid(process.pid());
            if (windows.isEmpty()) {
                return false;
            }

            for (NativeWinWindowControl window : windows) {
                var c = new ControllableWindowsProcess(process.toHandle(), window);
                if (!filter.test(c)) {
                    continue;
                }

                var entry = new RemoteDesktopDockEntry(name, icon, color, e, c, null, w, h);
                model.track(entry);
                return true;
            }

            return false;
        });
    }

    private ObservableStringValue createTitle() {
        return Bindings.createStringBinding(() -> {
            var selected = this.selected.get();
            if (selected == null) {
                return "Remote Desktop Dock";
            }

            var name = selected.getName();
            return name + " - Remote Desktop Connection";
        }, selected);
    }

    private void startStateListener() {
        GlobalTimer.scheduleUntil(Duration.ofMillis(200), false, () -> {
            if (stage == null) {
                return false;
            }

            updateState();
            return false;
        });
    }

    private void updateState() {
        if (restartTriggered.get()) {
            model.clearDead();
            DerivedObservableList.wrap(processes, true).setContent(model.getEntries());
            selected.set(model.getSelected());
            return;
        }

        var oldSize = processes.size();
        model.clearDead();
        DerivedObservableList.wrap(processes, true).setContent(model.getEntries());
        selected.set(model.getSelected());
        if (oldSize > 0 && processes.isEmpty()) {
            PlatformThread.runLaterIfNeededBlocking(() -> {
                stage.hide();
            });
        }
    }

    private void onWindowStateChange() {
        if (!stage.isShowing()) {
            return;
        }

        if (state != null && stage.isMaximized()) {
            state = state.toBuilder().maximized(true).build();
            return;
        }

        state = new State(false, stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
    }

    private void setupWindowListeners() {
        stage.xProperty().addListener((c, o, n) -> {
            onWindowStateChange();
        });
        stage.yProperty().addListener((c, o, n) -> {
            onWindowStateChange();
        });
        stage.widthProperty().addListener((c, o, n) -> {
            onWindowStateChange();
        });
        stage.heightProperty().addListener((c, o, n) -> {
            onWindowStateChange();
        });
        stage.maximizedProperty().addListener((c, o, n) -> {
            onWindowStateChange();
        });
    }

    public Rect getDockBounds() {
        return model.getViewBounds();
    }

    private static RemoteDesktopWindow INSTANCE;

    public static RemoteDesktopWindow get() {
        return INSTANCE;
    }

    @Builder(toBuilder = true)
    @Jacksonized
    @Value
    public static class State {
        boolean maximized;
        double windowX;
        double windowY;
        double windowWidth;
        double windowHeight;
    }
}
