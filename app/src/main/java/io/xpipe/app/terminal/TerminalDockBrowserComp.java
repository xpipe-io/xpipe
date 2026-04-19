package io.xpipe.app.terminal;

import io.xpipe.app.util.WindowDockComp;
import io.xpipe.app.comp.base.LoadingIconComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import org.kordamp.ikonli.javafx.FontIcon;

public class TerminalDockBrowserComp extends WindowDockComp<TerminalDockView> {

    private final ObservableBooleanValue opened;

    public TerminalDockBrowserComp(TerminalDockView model, ObservableBooleanValue opened) {
        super(model);
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
            model.attach();
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
                        var l = new LoadingIconComp(new SimpleBooleanProperty(true), AppFontSizes::sm).build();
                        label.setGraphic(l);
                    }
                }
            });
        });

        return stack;
    }
}
