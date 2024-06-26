package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.skin.ScrollPaneSkin;
import javafx.scene.layout.StackPane;

public class ScrollComp extends Comp<CompStructure<ScrollPane>> {

    private final Comp<?> content;

    public ScrollComp(Comp<?> content) {this.content = content;}

    @Override
    public CompStructure<ScrollPane> createBase() {
        var stack = new StackPane(content.createRegion());
        stack.getStyleClass().add("scroll-comp-content");

        var sp = new ScrollPane(stack);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-comp");
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setSkin(new ScrollPaneSkin(sp));

        ScrollBar bar = (ScrollBar) sp.lookup(".scroll-bar:vertical");
        bar.opacityProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> {
                            var v = bar.getVisibleAmount();
                            return v < 1.0 ? 1.0 : 0.0;
                        },
                        bar.visibleAmountProperty()));

        StackPane viewport = (StackPane) sp.lookup(".viewport");
        var child = viewport.getChildren().getFirst();
        child.getStyleClass().add("view");
        return new SimpleCompStructure<>(sp);
    }
}
