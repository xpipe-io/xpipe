package io.xpipe.app.comp.base;




import io.xpipe.app.comp.RegionStructure;
import io.xpipe.app.comp.RegionStructureBuilder;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Region;

import lombok.Value;
import org.int4.fx.builders.common.AbstractRegionBuilder;
import io.xpipe.app.comp.BaseRegionBuilder;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class LeftSplitPaneComp extends RegionStructureBuilder<SplitPane, LeftSplitPaneComp.Structure> {

    private final BaseRegionBuilder<?,?> left;
    private final BaseRegionBuilder<?,?> center;
    private Double initialWidth;
    private Consumer<Double> onDividerChange;

    public LeftSplitPaneComp(BaseRegionBuilder<?,?> left, BaseRegionBuilder<?,?> center) {
        this.left = left;
        this.center = center;
    }

    @Override
    public Structure createBase() {
        var c = center.build();
        var sidebar = left.build();
        if (initialWidth != null) {
            sidebar.setPrefWidth(initialWidth);
        }
        var r = new SplitPane(c);

        AtomicBoolean setInitial = new AtomicBoolean(false);
        r.widthProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() <= 0 || !r.getItems().contains(sidebar)) {
                return;
            }

            if (!setInitial.get() && initialWidth != null && r.getDividers().size() > 0) {
                r.getDividers().getFirst().setPosition(initialWidth / newValue.doubleValue());
                setInitial.set(true);
            }
        });

        var dividerPosition = new SimpleDoubleProperty();
        ChangeListener<Number> changeListener = (observable, oldValue, newValue) -> {
            if (r.getWidth() <= 0 || !r.getItems().contains(sidebar)) {
                return;
            }

            if (onDividerChange != null) {
                onDividerChange.accept(newValue.doubleValue() * r.getWidth());
            }

            dividerPosition.set(newValue.doubleValue() * r.getWidth());
        };

        sidebar.managedProperty().subscribe(m -> {
            var divs = r.getDividers();
            if (!m) {
                if (!divs.isEmpty()) {
                    divs.getFirst().positionProperty().removeListener(changeListener);
                }
                r.getItems().remove(sidebar);
                if (onDividerChange != null) {
                    onDividerChange.accept(0.0);
                }
            } else if (!r.getItems().contains(sidebar)) {
                r.getItems().addFirst(sidebar);
                var oldPos = dividerPosition.get();
                var d = oldPos / r.getWidth();
                divs.getFirst().setPosition(d);
                r.layout();
                Platform.runLater(() -> {
                    // Div might be removed again since last time
                    if (divs.size() > 0) {
                        divs.getFirst().setPosition(oldPos / r.getWidth());
                    }
                });
                if (onDividerChange != null) {
                    onDividerChange.accept(d);
                }
                divs.getFirst().positionProperty().addListener(changeListener);
            }
        });

        SplitPane.setResizableWithParent(sidebar, false);
        r.getStyleClass().add("side-split-pane-comp");
        return new Structure(sidebar, c, r);
    }

    public LeftSplitPaneComp withInitialWidth(double val) {
        this.initialWidth = val;
        return this;
    }

    public LeftSplitPaneComp withOnDividerChange(Consumer<Double> onDividerChange) {
        this.onDividerChange = onDividerChange;
        return this;
    }

    @Value
    public static class Structure implements RegionStructure<SplitPane> {

        Region left;
        Region center;
        SplitPane pane;

        @Override
        public SplitPane get() {
            return pane;
        }
    }
}
