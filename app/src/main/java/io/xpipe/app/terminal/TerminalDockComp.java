package io.xpipe.app.terminal;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.WindowEvent;

import atlantafx.base.controls.RingProgressIndicator;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.concurrent.atomic.AtomicReference;

public class TerminalDockComp extends SimpleComp {

    private final TerminalDockModel model;
    private final ObservableBooleanValue opened;

    public TerminalDockComp(TerminalDockModel model, ObservableBooleanValue opened) {
        this.model = model;
        this.opened = opened;
    }

    @Override
    protected Region createSimple() {
        var label = new Label();
        AppFontSizes.xl(label);
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

        opened.subscribe(v -> {
            PlatformThread.runLaterIfNeeded(() -> {
                label.textProperty().unbind();
                if (v) {
                    stack.pseudoClassStateChanged(PseudoClass.getPseudoClass("empty"), false);
                    label.textProperty().bind(AppI18n.observable("clickToDock"));
                    label.setGraphic(new FontIcon("mdi2d-dock-right"));
                } else {
                    stack.pseudoClassStateChanged(PseudoClass.getPseudoClass("empty"), true);
                    label.textProperty().bind(AppI18n.observable("terminalStarting"));
                    if (!AppPrefs.get().performanceMode().get()) {
                        var i = new RingProgressIndicator(-1.0, false);
                        i.setMaxWidth(10);
                        i.setMaxHeight(10);
                        label.setGraphic(i);
                    }
                }
            });
        });

        return stack;
    }

    private void setupListeners(StackPane stack) {
        var s = AppMainWindow.getInstance().getStage();

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
                    model.onWindowActivate();
                }
            }
        };
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
                (int) Math.ceil(bounds.getMinX() * sx + p.getLeft()),
                (int) Math.ceil(bounds.getMinY() * sy + p.getTop()),
                (int) Math.floor(bounds.getWidth() * sx - p.getRight() - p.getLeft()),
                (int) Math.floor(bounds.getHeight() * sy - p.getBottom() - p.getTop()));
    }
}
