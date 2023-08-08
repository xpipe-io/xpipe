package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

public class DynamicOptionsComp extends Comp<CompStructure<Pane>> {

    private final List<Entry> entries;
    private final boolean wrap;

    public DynamicOptionsComp(List<Entry> entries, boolean wrap) {
        this.entries = entries;
        this.wrap = wrap;
    }

    public Entry queryEntry(String key) {
        return entries.stream()
                .filter(entry -> entry.key != null && entry.key.equals(key))
                .findAny()
                .orElseThrow();
    }

    @Override
    public CompStructure<Pane> createBase() {
        Pane pane;
        if (wrap) {
            var content = new FlowPane(Orientation.HORIZONTAL);
            content.setAlignment(Pos.CENTER);
            content.setHgap(14);
            content.setVgap(7);
            pane = content;
        } else {
            var content = new VBox();
            content.setSpacing(7);
            pane = content;
        }

        var nameRegions = new ArrayList<Region>();
        var compRegions = new ArrayList<Region>();

        for (var entry : getEntries()) {
            Region compRegion = null;
            if (entry.comp() != null) {
                compRegion = entry.comp().createRegion();
            }

            if (entry.name() != null) {
                var line = new HBox();
                line.setFillHeight(true);
                if (!wrap) {
                    line.prefWidthProperty().bind(pane.widthProperty());
                }
                line.setSpacing(8);

                var name = new Label();
                name.textProperty().bind(entry.name());
                name.prefHeightProperty().bind(line.heightProperty());
                name.setMinWidth(Region.USE_PREF_SIZE);
                name.setAlignment(Pos.CENTER_LEFT);
                if (compRegion != null) {
                    name.visibleProperty().bind(PlatformThread.sync(compRegion.visibleProperty()));
                    name.managedProperty().bind(PlatformThread.sync(compRegion.managedProperty()));
                }
                nameRegions.add(name);
                line.getChildren().add(name);

                if (compRegion != null) {
                    compRegions.add(compRegion);
                    line.getChildren().add(compRegion);
                    if (!wrap) {
                        HBox.setHgrow(compRegion, Priority.ALWAYS);
                    }
                }

                pane.getChildren().add(line);
            } else {
                if (compRegion != null) {
                    compRegions.add(compRegion);
                    pane.getChildren().add(compRegion);
                }
            }
        }

        if (wrap) {
            var compWidthBinding = Bindings.createDoubleBinding(
                    () -> {
                        if (compRegions.stream().anyMatch(r -> r.getWidth() == 0)) {
                            return Region.USE_COMPUTED_SIZE;
                        }

                        return compRegions.stream()
                                .map(Region::getWidth)
                                .max(Double::compareTo)
                                .orElse(0.0);
                    },
                    compRegions.stream().map(Region::widthProperty).toList().toArray(new Observable[0]));
            compRegions.forEach(r -> r.prefWidthProperty().bind(compWidthBinding));
        }

        if (entries.stream().anyMatch(entry -> entry.name() != null)) {
            var nameWidthBinding = Bindings.createDoubleBinding(
                    () -> {
                        if (nameRegions.stream().anyMatch(r -> r.getWidth() == 0)) {
                            return Region.USE_COMPUTED_SIZE;
                        }

                        return nameRegions.stream()
                                .map(Region::getWidth)
                                .max(Double::compareTo)
                                .orElse(0.0);
                    },
                    nameRegions.stream().map(Region::widthProperty).toList().toArray(new Observable[0]));
            nameRegions.forEach(r -> r.prefWidthProperty().bind(nameWidthBinding));
        }

        return new SimpleCompStructure<>(pane);
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public record Entry(String key, ObservableValue<String> name, Comp<?> comp) {}
}
