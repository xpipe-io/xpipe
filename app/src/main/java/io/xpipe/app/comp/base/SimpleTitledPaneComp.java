package io.xpipe.app.comp.base;



import io.xpipe.app.comp.RegionBuilder;


import javafx.beans.value.ObservableValue;
import javafx.scene.control.TitledPane;
import org.int4.fx.builders.common.AbstractRegionBuilder;
import io.xpipe.app.comp.BaseRegionBuilder;

public class SimpleTitledPaneComp extends RegionBuilder<TitledPane> {

    private final ObservableValue<String> name;
    private final BaseRegionBuilder<?,?> content;
    private final boolean collapsible;

    public SimpleTitledPaneComp(ObservableValue<String> name, BaseRegionBuilder<?,?> content, boolean collapsible) {
        this.name = name;
        this.content = content;
        this.collapsible = collapsible;
    }

    @Override
    public TitledPane createSimple() {
        var r = content.build();
        r.getStyleClass().add("content");
        var tp = new TitledPane(null, r);
        tp.textProperty().bind(name);
        tp.getStyleClass().add("simple-titled-pane-comp");
        tp.setExpanded(true);
        tp.setCollapsible(collapsible);
        return tp;
    }
}
