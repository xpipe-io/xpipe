package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;

import javafx.beans.binding.Bindings;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.skin.ScrollPaneSkin;
import javafx.scene.layout.StackPane;

public class ScrollComp extends Comp<CompStructure<ScrollPane>> {

    private final Comp<?> content;

    public ScrollComp(Comp<?> content) {
        this.content = content;
    }

    @Override
    public CompStructure<ScrollPane> createBase() {
        var r = content.createRegion();
        var stack = new StackPane(r);
        stack.getStyleClass().add("scroll-comp-content");

        var sp = new ScrollPane(stack);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-comp");
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setSkin(new ScrollPaneSkin(sp));

        ScrollBar bar = (ScrollBar) sp.lookup(".scroll-bar:vertical");
        bar.opacityProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> {
                            var v = bar.getVisibleAmount();
                            // Check for rounding and accuracy issues
                            // It might not be exactly equal to 1.0
                            return v < 0.99 ? 1.0 : 0.0;
                        },
                        bar.visibleAmountProperty()));

        StackPane viewport = (StackPane) sp.lookup(".viewport");
        var child = viewport.getChildren().getFirst();
        child.getStyleClass().add("view");
        return new SimpleCompStructure<>(sp);
    }
}
