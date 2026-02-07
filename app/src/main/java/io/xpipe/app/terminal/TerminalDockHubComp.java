package io.xpipe.app.terminal;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.core.window.AppMainWindow;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.WindowEvent;

import org.kordamp.ikonli.javafx.FontIcon;

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
                        model.onWindowActivate();
                    });
                }
            }
        };
        var focus = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    var selected = s.getScene().getRoot().lookup(".icon-button-comp:hover");
                    if (selected instanceof Button b
                            && b.getGraphic() instanceof FontIcon fi
                            && !fi.getIconLiteral().equals("mdi2c-connection")) {
                        return;
                    }

                    model.onFocusGain();
                } else {
                    Platform.runLater(() -> {
                        model.onFocusLost();
                    });
                }
            }
        };
        var show = new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                update(stack);
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
                s.focusedProperty().removeListener(focus);
                s.removeEventFilter(WindowEvent.WINDOW_SHOWN, show);
                s.removeEventFilter(WindowEvent.WINDOW_HIDING, hide);
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
                s.focusedProperty().addListener(focus);
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
        if (region.getScene() == null || region.getScene().getWindow() == null) {
            return;
        }

        var bounds = region.localToScreen(region.getBoundsInLocal());
        var p = region.getPadding();
        var sx = region.getScene().getWindow().getOutputScaleX();
        var sy = region.getScene().getWindow().getOutputScaleY();
        model.resizeView(
                (int) Math.round(bounds.getMinX() * sx + p.getLeft()),
                (int) Math.round(bounds.getMinY() * sy + p.getTop()),
                (int) Math.round(bounds.getWidth() * sx - p.getRight() - p.getLeft()),
                (int) Math.round(bounds.getHeight() * sy - p.getBottom() - p.getTop()));
    }
}
