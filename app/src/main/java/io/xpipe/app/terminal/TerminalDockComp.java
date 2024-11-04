package io.xpipe.app.terminal;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.fxcomps.SimpleComp;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.WindowEvent;

public class TerminalDockComp extends SimpleComp {
    
    private final TerminalDockModel model;

    public TerminalDockComp(TerminalDockModel model) {this.model = model;}

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
        var s = AppMainWindow.getInstance().getStage();
        s.xProperty().addListener((observable, oldValue, newValue) -> {
            update(stack);
        });
        s.yProperty().addListener((observable, oldValue, newValue) -> {
            update(stack);
        });
        s.widthProperty().addListener((observable, oldValue, newValue) -> {
            update(stack);
        });
        s.heightProperty().addListener((observable, oldValue, newValue) -> {
            update(stack);
        });
        s.iconifiedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                model.onWindowMinimize();
            } else {
                model.onWindowActivate();
            }
        });
        s.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                model.onFocusGain();
            } else {
                model.onFocusLost();
            }
        });
        s.addEventFilter(WindowEvent.WINDOW_SHOWN,event -> {
            update(stack);
        });
        s.addEventFilter(WindowEvent.WINDOW_HIDING,event -> {
            model.onClose();
        });
        stack.setOnMouseClicked(event -> {
            model.clickView();
            event.consume();
        });
        return stack;
    }

    private void update(Region region) {
        var bounds = region.localToScreen(region.getBoundsInLocal());
        var sx = region.getScene().getWindow().getOutputScaleX();
        var sy = region.getScene().getWindow().getOutputScaleY();
        model.resizeView((int) Math.ceil(bounds.getMinX() * sx), (int) Math.ceil(bounds.getMinY() * sy),(int) Math.floor(bounds.getWidth() * sx), (int) Math.floor(bounds.getHeight() * sy));
    }
}
