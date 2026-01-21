package io.xpipe.app.comp.base;




import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.stream.Collectors;

public class IntroListComp extends SimpleRegionBuilder {

    private final List<IntroComp> intros;

    public IntroListComp(List<IntroComp> intros) {
        this.intros = intros;
    }

    @Override
    public Region createSimple() {
        var v = new VerticalComp(intros).build();
        v.setSpacing(80);
        v.setMinWidth(Region.USE_PREF_SIZE);
        v.setMaxWidth(Region.USE_PREF_SIZE);
        v.setMinHeight(Region.USE_PREF_SIZE);
        v.setMaxHeight(Region.USE_PREF_SIZE);

        var sp = new StackPane(v);
        sp.setPadding(new Insets(40, 0, 0, 0));
        sp.setAlignment(Pos.CENTER);
        sp.setPickOnBounds(false);
        return sp;
    }
}
