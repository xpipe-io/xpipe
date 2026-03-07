package io.xpipe.app.terminal;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.core.window.AppMainWindow;

import io.xpipe.app.platform.NativeWinWindowControl;
import io.xpipe.app.util.GlobalTimer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.WindowEvent;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

public class TerminalDockHubComp extends SimpleRegionBuilder {

    private final TerminalDockView model;

    public TerminalDockHubComp(TerminalDockView model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var stack = new StackPane();
        stack.setPickOnBounds(false);
        stack.boundsInParentProperty().addListener((observable, oldValue, newValue) -> {
            update(stack);
        });
        stack.getStyleClass().add("terminal-dock-comp");
        stack.setMinWidth(100);
        stack.setMinHeight(100);
        setupListeners(stack);
        return stack;
    }

    private void setupListeners(StackPane stack) {
        var s = AppMainWindow.get().getStage();

        var bounds = new ChangeListener<Bounds>() {
            @Override
            public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
                update(stack);
            }
        };
        var scale = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                GlobalTimer.delay(() -> {
                    Platform.runLater(() -> {
                        update(stack);
                    });
                }, Duration.ofMillis(500));
            }
        };
        var update = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                update(stack);
            }
        };
        var iconified = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    model.onWindowMinimize();
                } else {
                    Platform.runLater(() -> {
                        model.onWindowShow();
                    });
                }
            }
        };
        var show = new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                GlobalTimer.delay(() -> {
                    Platform.runLater(() -> {
                        update(stack);
                    });
                }, Duration.ofMillis(100));
            }
        };
        var hide = new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                model.onClose();
            }
        };

        var parent = new AtomicReference<Parent>();
        stack.sceneProperty().subscribe(scene -> {
            if (scene == null) {
                s.xProperty().removeListener(update);
                s.yProperty().removeListener(update);
                s.widthProperty().removeListener(update);
                s.heightProperty().removeListener(update);
                s.iconifiedProperty().removeListener(iconified);
                s.removeEventFilter(WindowEvent.WINDOW_SHOWN, show);
                s.removeEventFilter(WindowEvent.WINDOW_HIDING, hide);
                s.outputScaleXProperty().addListener(scale);
                if (parent.get() != null) {
                    parent.get().boundsInParentProperty().removeListener(bounds);
                    parent.set(null);
                }
            } else {
                s.xProperty().addListener(update);
                s.yProperty().addListener(update);
                s.widthProperty().addListener(update);
                s.heightProperty().addListener(update);
                s.iconifiedProperty().addListener(iconified);
                s.outputScaleXProperty().removeListener(scale);
                s.addEventFilter(WindowEvent.WINDOW_SHOWN, show);
                s.addEventFilter(WindowEvent.WINDOW_HIDING, hide);
                // As in practice this node is wrapped in another stack pane
                // We have to listen to the parent bounds to actually receive bounds changes
                stack.getParent().boundsInParentProperty().addListener(bounds);
                parent.set(stack.getParent());
                update(stack);
            }
        });
    }

    private void update(Region region) {
        if (region.getScene() == null || region.getScene().getWindow() == null || NativeWinWindowControl.MAIN_WINDOW == null) {
            return;
        }

        var bounds = region.localToScene(region.getBoundsInLocal());
        var p = region.getPadding();
        var sx = region.getScene().getWindow().getOutputScaleX();
        var sy = region.getScene().getWindow().getOutputScaleY();

        var scene =  region.getScene();
        var windowRect = NativeWinWindowControl.MAIN_WINDOW.getBounds();
        if (windowRect.getX() == 0.0 && windowRect.getY() == 0.0 && windowRect.getW() == 0 && windowRect.getH() == 0) {
            return;
        }

        var xPadding = ((bounds.getMinX() + p.getLeft() + scene.getX()) * sx);
        var yPadding = ((bounds.getMinY() + p.getTop() + scene.getY()) * sy);
        var x = windowRect.getX() + xPadding;
        var y = windowRect.getY() + yPadding;
        var w = (bounds.getWidth() * sx) - p.getRight() - p.getLeft();
        var h = (bounds.getHeight() * sy) - p.getBottom() - p.getTop();

        if (x + w > windowRect.getX() + windowRect.getW()) {
            x = windowRect.getX() + 10;
            w = windowRect.getW() - 20;
        }
        if (y + h > windowRect.getY() + windowRect.getH()) {
            y = windowRect.getY() + 10;
            h = windowRect.getH() - 20;
        }

        model.resizeView(
                (int) Math.round(x),
                (int) Math.round(y),
                (int) Math.round(w),
                (int) Math.round(h));
    }
}
