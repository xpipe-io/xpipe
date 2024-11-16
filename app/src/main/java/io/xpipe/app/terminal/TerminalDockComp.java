package io.xpipe.app.terminal;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppMainWindow;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.WindowEvent;

public class TerminalDockComp extends SimpleComp {

    private final TerminalDockModel model;

    public TerminalDockComp(TerminalDockModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var label = new Label();
        label.textProperty().bind(AppI18n.observable("clickToDock"));
        var stack = new StackPane(label);
        stack.setAlignment(Pos.CENTER);
        stack.setCursor(Cursor.HAND);
        stack.boundsInParentProperty().addListener((observable, oldValue, newValue) -> {
            update(stack);
        });
        stack.setOnMouseClicked(event -> {
            model.clickView();
            event.consume();
        });
        stack.getStyleClass().add("terminal-dock-comp");
        stack.setMinWidth(100);
        stack.setMinHeight(100);
        setupListeners(stack);
        return stack;
    }

    private void setupListeners(StackPane stack) {
        var s = AppMainWindow.getInstance().getStage();

        var update = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                update(stack);
            }
        };
        s.xProperty().addListener(update);
        s.yProperty().addListener(update);
        s.widthProperty().addListener(update);
        s.heightProperty().addListener(update);

        var iconified = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    model.onWindowMinimize();
                } else {
                    model.onWindowActivate();
                }
            }
        };
        s.iconifiedProperty().addListener(iconified);

        var focus = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    model.onFocusGain();
                } else {
                    model.onFocusLost();
                }
            }
        };
        s.focusedProperty().addListener(focus);

        var show = new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                update(stack);
            }
        };
        s.addEventFilter(WindowEvent.WINDOW_SHOWN, show);

        var hide = new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                update(stack);
            }
        };
        s.addEventFilter(WindowEvent.WINDOW_HIDING, hide);

        stack.sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null && newValue == null) {
                s.xProperty().removeListener(update);
                s.yProperty().removeListener(update);
                s.widthProperty().removeListener(update);
                s.heightProperty().removeListener(update);
                s.iconifiedProperty().removeListener(iconified);
                s.focusedProperty().removeListener(focus);
                s.removeEventFilter(WindowEvent.WINDOW_SHOWN, show);
                s.removeEventFilter(WindowEvent.WINDOW_HIDING, hide);
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
                (int) Math.ceil(bounds.getMinX() * sx + p.getLeft()),
                (int) Math.ceil(bounds.getMinY() * sy + p.getTop()),
                (int) Math.floor(bounds.getWidth() * sx - p.getRight() - p.getLeft()),
                (int) Math.floor(bounds.getHeight() * sy - p.getBottom() - p.getTop()));
    }
}
