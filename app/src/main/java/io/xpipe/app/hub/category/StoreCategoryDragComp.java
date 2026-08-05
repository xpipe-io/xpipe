package io.xpipe.app.hub.category;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.core.AppStyle;
import io.xpipe.app.core.window.AppWindowStyle;
import io.xpipe.app.platform.LabelGraphic;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class StoreCategoryDragComp extends SimpleRegionBuilder {

    private final ObservableList<StoreCategoryWrapper> list;

    public static Image snapshot(List<StoreCategoryWrapper> list) {
        var r = new StoreCategoryDragComp(FXCollections.observableList(list))
                .style("drag-comp")
                .build();
        var scene = new Scene(r);
        AppWindowStyle.addStylesheets(scene);
        AppStyle.addStylesheets(scene);
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return r.snapshot(parameters, null);
    }

    @Override
    protected Region createSimple() {
        var c = new ListBoxViewComp<>(
                list,
                list,
                entry -> {
                    return RegionBuilder.of(() -> {
                        var icon = entry.getIconFile().getValue();
                        var label = new LabelComp(entry.getName().getValue(), new LabelGraphic.ImageGraphic(icon, 16));
                        return label.build();
                    });
                },
                false);
        return c.build();
    }
}
