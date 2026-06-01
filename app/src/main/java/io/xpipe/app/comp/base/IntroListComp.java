package io.xpipe.app.comp.base;

import io.xpipe.app.comp.SimpleRegionBuilder;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.List;

public class IntroListComp extends SimpleRegionBuilder {

    private final List<IntroComp> intros;

    public IntroListComp(List<IntroComp> intros) {
        this.intros = intros;
    }

    @Override
    public Region createSimple() {
        var v = new VerticalComp(intros).build();
        v.setMaxHeight(Region.USE_PREF_SIZE);
        var sp = new StackPane(v);
        sp.setPadding(new Insets(40, 0, 0, 0));
        sp.setAlignment(Pos.CENTER);
        sp.setPickOnBounds(false);
        return sp;
    }
}
