package io.xpipe.app.hub.list;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppStyle;
import io.xpipe.app.core.window.AppWindowStyle;
import io.xpipe.app.hub.entry.StoreEntryWrapper;
import io.xpipe.app.platform.LabelGraphic;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class StoreSectionDragComp extends SimpleRegionBuilder {

    private final ObservableList<StoreEntryWrapper> list;

    public static Image snapshot(List<StoreEntryWrapper> list) {
        var copy = new ArrayList<>(list);
        var filtered = new ArrayList<>(list);
        filtered.removeIf(wrapper -> {
            var current = wrapper;
            while ((current = StoreViewState.get()
                            .getParentSectionForWrapper(current)
                            .map(storeSection -> storeSection.getWrapper())
                            .orElse(null))
                    != null) {
                if (copy.contains(current)) {
                    return true;
                }
            }
            return false;
        });

        var r = new StoreSectionDragComp(FXCollections.observableList(filtered));

        var label = new LabelComp(AppI18n.observable("orderDisabledNotice"), new ReadOnlyObjectWrapper<>(new LabelGraphic.IconGraphic("mdi2i-information-outline")));
        label.hide(Bindings.createBooleanBinding(() -> {
            return StoreViewState.get().getSortMode().getValue().supportsReordering();
        }, StoreViewState.get().getSortMode()));
        label.apply(struc -> struc.setWrapText(true));
        label.maxWidth(200);

        var vbox = new VerticalComp(List.of(r, label))
                .style("drag-comp").build();

        var scene = new Scene(vbox);
        AppWindowStyle.addStylesheets(scene);
        AppStyle.addStylesheets(scene);
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return vbox.snapshot(parameters, null);
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
