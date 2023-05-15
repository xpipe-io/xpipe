package io.xpipe.app.browser;

import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class BrowserNavBar extends SimpleComp {

    private static final PseudoClass INVISIBLE = PseudoClass.getPseudoClass("invisible");

    private final OpenFileSystemModel model;

    public BrowserNavBar(OpenFileSystemModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var path = new SimpleStringProperty(model.getCurrentPath().get());
        path.addListener((observable, oldValue, newValue) -> {
            var changed = model.cd(newValue);
            changed.ifPresent(path::set);
        });
        var pathBar = new TextFieldComp(path, true).createRegion();
        pathBar.getStyleClass().add("path-text");
        model.getCurrentPath().addListener((observable, oldValue, newValue) -> {
            path.set(newValue);
        });
        SimpleChangeListener.apply(pathBar.focusedProperty(), val -> {
            pathBar.pseudoClassStateChanged(INVISIBLE, !val);
        });

        var breadcrumbs = new FileBrowserBreadcrumbBar(model)
                .hide(pathBar.focusedProperty())
                .createRegion();

        var stack = new StackPane(pathBar, breadcrumbs);
        HBox.setHgrow(stack, Priority.ALWAYS);
        stack.setAlignment(Pos.CENTER_LEFT);

        return stack;
    }
}
