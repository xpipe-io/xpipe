package io.xpipe.extension.comp;

import io.xpipe.fxcomps.Comp;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.SimpleCompStructure;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;

public class DynamicOptionsComp extends Comp<CompStructure<FlowPane>> {

    private final List<Entry> entries;
    private final boolean wrap;

    public DynamicOptionsComp(List<Entry> entries, boolean wrap) {
        this.entries = entries;
        this.wrap = wrap;
    }

    @Override
    public CompStructure<FlowPane> createBase() {
        var flow = new FlowPane(Orientation.HORIZONTAL);
        flow.setAlignment(Pos.CENTER);
        flow.setHgap(7);
        flow.setVgap(7);

        var nameRegions = new ArrayList<Region>();
        var compRegions = new ArrayList<Region>();

        for (var entry : getEntries()) {
            var line = new HBox();
            line.setFillHeight(true);
            if (!wrap) {
                line.prefWidthProperty().bind(flow.widthProperty());
            }
            line.setSpacing(8);

            if (entry.name() != null) {
                var name = new Label();
                name.textProperty().bind(entry.name());
                name.prefHeightProperty().bind(line.heightProperty());
                name.setMinWidth(Region.USE_PREF_SIZE);
                name.setAlignment(Pos.CENTER_LEFT);
                nameRegions.add(name);
                line.getChildren().add(name);
            }

            var r = entry.comp().createRegion();
            compRegions.add(r);
            line.getChildren().add(r);
            if (!wrap) {
                HBox.setHgrow(r, Priority.ALWAYS);
            }

            flow.getChildren().add(line);
        }

        if (wrap) {
            var compWidthBinding = Bindings.createDoubleBinding(
                    () -> {
                        if (compRegions.stream().anyMatch(r -> r.getWidth() == 0)) {
                            return Region.USE_COMPUTED_SIZE;
                        }

                        var m = compRegions.stream()
                                .map(Region::getWidth)
                                .max(Double::compareTo)
                                .orElse(0.0);
                        return m;
                    },
                    compRegions.stream().map(Region::widthProperty).toList().toArray(new Observable[0]));
            compRegions.forEach(r -> r.prefWidthProperty().bind(compWidthBinding));
        }

        var nameWidthBinding = Bindings.createDoubleBinding(
                () -> {
                    if (nameRegions.stream().anyMatch(r -> r.getWidth() == 0)) {
                        return Region.USE_COMPUTED_SIZE;
                    }

                    var m = nameRegions.stream()
                            .map(Region::getWidth)
                            .max(Double::compareTo)
                            .orElse(0.0);
                    return m;
                },
                nameRegions.stream().map(Region::widthProperty).toList().toArray(new Observable[0]));
        nameRegions.forEach(r -> r.prefWidthProperty().bind(nameWidthBinding));

        return new SimpleCompStructure<>(flow);
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public static record Entry(ObservableValue<String> name, Comp<?> comp) {}
}
