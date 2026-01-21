package io.xpipe.app.comp.base;




import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.platform.PlatformThread;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import org.int4.fx.builders.common.AbstractRegionBuilder;
import io.xpipe.app.comp.BaseRegionBuilder;

import java.util.List;

public class HorizontalComp extends RegionBuilder<HBox> {

    private final ObservableList<BaseRegionBuilder<?,?>> entries;

    public HorizontalComp(List<BaseRegionBuilder<?,?>> comps) {
        entries = FXCollections.observableArrayList(List.copyOf(comps));
    }

    public HorizontalComp(ObservableList<BaseRegionBuilder<?,?>> entries) {
        this.entries = PlatformThread.sync(entries);
    }

    public RegionBuilder<HBox> spacing(double spacing) {
        return apply(struc -> struc.setSpacing(spacing));
    }

    @Override
    public HBox createSimple() {
        var b = new HBox();
        b.getStyleClass().add("horizontal-comp");
        entries.addListener((ListChangeListener<? super BaseRegionBuilder<?,?>>) c -> {
            b.getChildren().setAll(c.getList().stream().map(ab -> ab.build()).toList());
        });
        for (var entry : entries) {
            b.getChildren().add(entry.build());
        }
        b.setAlignment(Pos.CENTER);
        return b;
    }
}
