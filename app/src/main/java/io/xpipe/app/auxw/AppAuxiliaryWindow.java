package io.xpipe.app.auxw;

import io.xpipe.app.core.*;
import io.xpipe.app.core.window.AppWindowStyle;
import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.Rect;
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
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;

public class AppAuxiliaryWindow {

    private AppAuxiliaryWindow(State state, AuxDockImpl model) {
        this.state = state;
        this.model = model;
    }

    public static boolean isSupported() {
        return OsType.ofLocal() == OsType.WINDOWS;
    }

    public static void init() {
        if (!isSupported()) {
            return;
        }

        State state = AppCache.getNonNull("auxiliaryWindowState", State.class, () -> null);
        var model = new AuxDockImpl(rect -> rect, () -> {
            return INSTANCE.nativeWinWindowControl;
        });
        INSTANCE = new AppAuxiliaryWindow(state, model);
        INSTANCE.startStateListener();
    }

    private State state;
    private Stage stage;
    private NativeWinWindowControl nativeWinWindowControl;

    @Getter
    private final AuxDockImpl model;

    @Getter
    private final ObjectProperty<AuxEntry> selected = new SimpleObjectProperty<>();

    @Getter
    private final ObservableList<AuxEntry> processes = FXCollections.observableArrayList();

    @Getter
    private final BooleanProperty locked = new SimpleBooleanProperty();

    private void createStage() {
        if (stage != null) {
            return;
        }

        stage = new Stage();
        var scene = new Scene(new Region());
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.getScene().setRoot(new AuxDockCompImpl().build());
        stage.setWidth(1280);
        stage.setHeight(780);
        stage.titleProperty().bind(PlatformThread.sync(createTitle()));

        if (AppPrefs.get() != null) {
            stage.opacityProperty().bind(PlatformThread.sync(AppPrefs.get().windowOpacity()));
        }
        AppWindowStyle.addIcons(stage);
        AppWindowStyle.addStylesheets(stage.getScene());
        AppWindowStyle.addMaximizedPseudoClass(stage);
        AppWindowStyle.addFontSize(stage);
        AppTheme.initThemeHandlers(stage);

        if (state != null) {
            if (state.maximized) {
                stage.setMaximized(true);
            } else {
                stage.setX(state.windowX);
                stage.setY(state.windowY);
                stage.setWidth(state.windowWidth);
                stage.setHeight(state.windowHeight);
            }
        }

        setupWindowListeners();
    }

    public void show() {
        PlatformThread.runLaterIfNeededBlocking(() -> {
            createStage();
            stage.show();
            nativeWinWindowControl = new NativeWinWindowControl(stage);
            nativeWinWindowControl.setWindowsTransitionsEnabled(false);
        });
    }

    public void focus() {
        PlatformThread.runLaterIfNeeded(() -> {
            model.focus();
        });
    }

    public void select(AuxEntry entry) {
        model.select(entry);
        selected.set(entry);
    }

    public void close(AuxEntry entry) {
        model.closeWindow(entry);
    }

    public void toggleLock() {
        locked.set(!locked.get());
        state = state.toBuilder().locked(locked.get()).build();
    }

    public void track(String name, String icon, DataStoreColor color, Process process, Duration maxWait, Predicate<ControllableWindowProcess> filter) {
        var start = Instant.now();
        GlobalTimer.scheduleUntil(Duration.ofMillis(200), false, () -> {
            if (Duration.between(start, Instant.now()).compareTo(maxWait) > 0) {
                return true;
            }

            var windows = NativeWinWindowControl.byPid(process.pid());
            if (windows.isEmpty()) {
                return false;
            }

            for (NativeWinWindowControl window : windows) {
                var c = new ControllableWindowsProcess(process.toHandle(), window);
                if (!filter.test(c)) {
                    continue;
                }

                var entry = new AuxEntry(name, icon, color, c);
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
        GlobalTimer.scheduleUntil(Duration.ofMillis(500), false, () -> {
            if (stage == null || !stage.isShowing()) {
                return false;
            }

            updateState();
            return false;
        });
    }

    private void updateState() {
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
        state = new State(state != null && state.locked, stage.isMaximized(), stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
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
            locked.set(false);
        });
        stage.heightProperty().addListener((c, o, n) -> {
            onWindowStateChange();
            locked.set(false);
        });
        stage.maximizedProperty().addListener((c, o, n) -> {
            onWindowStateChange();
            locked.set(false);
            Platform.runLater(() -> {
                stage.setWidth(state.getWindowWidth());
                stage.setHeight(state.getWindowHeight());
            });
        });
        locked.addListener((v, o, n) -> {
            stage.setResizable(!n);
        });
    }

    public Rect getDockBounds() {
        return model.getViewBounds();
    }

    private static AppAuxiliaryWindow INSTANCE;

    public static AppAuxiliaryWindow get() {
        return INSTANCE;
    }

    @Builder(toBuilder = true)
    @Jacksonized
    @Value
    public static class State {
        boolean locked;
        boolean maximized;
        double windowX;
        double windowY;
        double windowWidth;
        double windowHeight;
    }
}
