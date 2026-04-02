package io.xpipe.app.terminal;

import io.xpipe.app.auxw.WindowDockComp;

import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class TerminalDockHubComp extends WindowDockComp<TerminalDockView> {

    public TerminalDockHubComp(TerminalDockView model) {
        super(model);
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
}
