package io.xpipe.app.terminal;

import io.xpipe.app.util.WindowDockComp;

import javafx.application.Platform;
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
        stack.sceneProperty().subscribe(scene -> {
            if (scene == null) {
                return;
            }

            var window = scene.getWindow();
            window.focusedProperty().subscribe(focus -> {
                if (!model.isActive()) {
                    return;
                }

               if (focus) {
                   var target = scene.getRoot().lookup(".icon-button-comp:hover");
                   if (target == null) {
                       Platform.runLater(() -> {
                           model.focus();
                       });
                   }
               }
            });
        });
        return stack;
    }
}
