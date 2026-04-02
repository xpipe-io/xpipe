package io.xpipe.app.auxw;

import io.xpipe.app.core.*;
import io.xpipe.app.core.window.AppModifiedStage;
import io.xpipe.app.core.window.AppWindowStyle;
import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.Rect;
import io.xpipe.core.OsType;
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
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;

public class AppAuxiliaryWindow {

    private AppAuxiliaryWindow(WindowState state, AuxDockImpl model) {
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

        WindowState state = AppCache.getNonNull("auxiliaryWindowState", WindowState.class, () -> null);
        var model = new AuxDockImpl(rect -> rect, () -> {
            return INSTANCE.nativeWinWindowControl;
        });
        INSTANCE = new AppAuxiliaryWindow(state, model);
        INSTANCE.startStateListener();
    }

    private WindowState state;
    private Stage stage;
    private NativeWinWindowControl nativeWinWindowControl;

    @Getter
    private final AuxDockImpl model;

    @Getter
    private final ObjectProperty<AuxEntry> selected = new SimpleObjectProperty<>();
    @Getter
    private final ObservableList<AuxEntry> processes = FXCollections.observableArrayList();

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

    public void track(String name, String icon, DataStoreColor color, Process process, Duration maxWait, Predicate<ControllableWindowProcess> filter) {
        var start = Instant.now();
        GlobalTimer.scheduleUntil(Duration.ofSeconds(1), false, () -> {
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
        selected.set(model.getSelected());
        DerivedObservableList.wrap(processes, true).setContent(model.getEntries());
        if (oldSize > 0 && processes.isEmpty()) {
            PlatformThread.runLaterIfNeededBlocking(() -> {
                stage.hide();
            });
        }
    }

    private void onWindowChange() {
        state = new WindowState(stage.isMaximized(), stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
    }

    private void setupWindowListeners() {
        stage.xProperty().addListener((c, o, n) -> {
            onWindowChange();
        });
        stage.yProperty().addListener((c, o, n) -> {
            onWindowChange();
        });
        stage.widthProperty().addListener((c, o, n) -> {
            onWindowChange();
        });
        stage.heightProperty().addListener((c, o, n) -> {
            onWindowChange();
        });
        stage.maximizedProperty().addListener((c, o, n) -> {
            onWindowChange();
        });
    }

    public Rect getDockBounds() {
        return model.getViewBounds();
    }

    private static AppAuxiliaryWindow INSTANCE;

    public static AppAuxiliaryWindow get() {
        return INSTANCE;
    }

    @Builder
    @Jacksonized
    @Value
    public static class WindowState {
        boolean maximized;
        double windowX;
        double windowY;
        double windowWidth;
        double windowHeight;
    }
}
