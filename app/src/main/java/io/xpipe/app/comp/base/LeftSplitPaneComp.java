package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Region;

import lombok.Value;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class LeftSplitPaneComp extends Comp<LeftSplitPaneComp.Structure> {

    private final Comp<?> left;
    private final Comp<?> center;
    private Double initialWidth;
    private Consumer<Double> onDividerChange;

    public LeftSplitPaneComp(Comp<?> left, Comp<?> center) {
        this.left = left;
        this.center = center;
    }

    @Override
    public Structure createBase() {
        var c = center.createRegion();
        var sidebar = left.createRegion();
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

            dividerPosition.set(newValue.doubleValue());
        };

        sidebar.managedProperty().subscribe(m -> {
            if (!m) {
                r.getDividers().getFirst().positionProperty().removeListener(changeListener);
                r.getItems().remove(sidebar);
                if (onDividerChange != null) {
                    onDividerChange.accept(0.0);
                }
            } else if (!r.getItems().contains(sidebar)) {
                r.getItems().addFirst(sidebar);
                var d = dividerPosition.get();
                r.getDividers().getFirst().setPosition(d);
                r.layout();
                if (onDividerChange != null) {
                    onDividerChange.accept(d);
                }
                r.getDividers().getFirst().positionProperty().addListener(changeListener);
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
    public static class Structure implements CompStructure<SplitPane> {

        Region left;
        Region center;
        SplitPane pane;

        @Override
        public SplitPane get() {
            return pane;
        }
    }
}
