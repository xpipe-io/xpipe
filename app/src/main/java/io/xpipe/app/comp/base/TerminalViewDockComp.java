package io.xpipe.app.comp.base;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.util.TerminalView;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.WindowEvent;

public class TerminalViewDockComp extends SimpleComp {

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
        stack.sceneProperty().addListener((observable, oldValue, newValue) -> {
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
                TerminalView.get().onMinimize();
            } else {
                TerminalView.get().onFocusGain();
            }
        });
        s.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                TerminalView.get().onFocusGain();
            } else {
                TerminalView.get().onFocusLost();
            }
        });
        s.addEventFilter(WindowEvent.WINDOW_SHOWN,event -> {
            update(stack);
        });
        s.addEventFilter(WindowEvent.WINDOW_HIDING,event -> {
            TerminalView.get().onClose();
        });
        stack.setOnMouseClicked(event -> {
            TerminalView.get().clickView();
            event.consume();
        });
        return stack;
    }

    private void update(Region region) {
        var bounds = region.localToScreen(region.getBoundsInLocal());
        TerminalView.get().resizeView((int) bounds.getMinX(), (int) bounds.getMinY(),(int) bounds.getWidth(), (int) bounds.getHeight());
    }
}
