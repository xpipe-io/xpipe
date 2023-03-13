package io.xpipe.app.comp.storage.store;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.LinkedHashMap;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = true)
public class StoreEntryFlatMiniSection extends SimpleComp {

    public static Map<StoreEntryWrapper, Region> createMap() {
        var map = new LinkedHashMap<StoreEntryWrapper, Region>();
        var topLevel = StoreViewSection.createTopLevels();
        var depth = 0;
        for (StoreViewSection v : topLevel) {
            add(depth, v, map);
        }
        return map;
    }

    private static void add(int depth, StoreViewSection section, Map<StoreEntryWrapper, Region> map) {
        map.put(section.getEntry(), new StoreEntryFlatMiniSection(depth, section.getEntry()).createRegion());
        for (StoreViewSection child : section.getChildren()) {
            add(depth + 1, child, map);
        }
    }

    int depth;
    StoreEntryWrapper wrapper;

    @Override
    protected Region createSimple() {
        var label = new Label(wrapper.getName(), new PrettyImageComp(new SimpleStringProperty(wrapper.getEntry().getProvider().getDisplayIconFileName()), 20, 20).createRegion());
        var spacer = new Spacer(depth * 10, Orientation.HORIZONTAL);
        var box = new HBox(spacer, label);
        return box;
    }
}
