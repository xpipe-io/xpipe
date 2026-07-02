package io.xpipe.app.comp.base;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;

import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.skin.ScrollPaneSkin;
import javafx.scene.layout.StackPane;

public class ScrollComp extends RegionBuilder<ScrollPane> {

    private final BaseRegionBuilder<?, ?> content;

    public ScrollComp(BaseRegionBuilder<?, ?> content) {
        this.content = content;
    }

    @Override
    public ScrollPane createSimple() {
        var r = content.build();
        var stack = new StackPane(r);
        stack.getStyleClass().add("scroll-comp-content");

        var sp = new ScrollPane(stack);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-comp");
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setSkin(new ScrollPaneSkin(sp));

        ScrollBar bar = (ScrollBar) sp.lookup(".scroll-bar:vertical");
        bar.visibleAmountProperty().subscribe(v -> {
            // Check for rounding and accuracy issues
            // It might not be exactly equal to 1.0
            var notNeeded = v.doubleValue() > 0.99;
            bar.pseudoClassStateChanged(PseudoClass.getPseudoClass("hidden"), notNeeded);
        });

        StackPane viewport = (StackPane) sp.lookup(".viewport");
        var child = viewport.getChildren().getFirst();
        child.getStyleClass().add("view");
        return sp;
    }
}
