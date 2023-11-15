package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Region;
import lombok.Value;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class SideSplitPaneComp extends Comp<SideSplitPaneComp.Structure> {

    private final Comp<?> left;
    private final Comp<?> center;
    private Double initialWidth;
    private Consumer<Double> onDividerChange;
    public SideSplitPaneComp(Comp<?> left, Comp<?> center) {
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
        var r = new SplitPane(sidebar, c);

        AtomicBoolean setInitial = new AtomicBoolean(false);
        r.widthProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() <= 0) {
                return;
            }

            if (!setInitial.get() && initialWidth != null) {
                r.getDividers().get(0).setPosition(initialWidth / newValue.doubleValue());
                setInitial.set(true);
            }
        });

        SplitPane.setResizableWithParent(sidebar, false);
        r.getDividers().get(0).positionProperty().addListener((observable, oldValue, newValue) -> {
            if (r.getWidth() <= 0) {
                return;
            }

            if (onDividerChange != null) {
                onDividerChange.accept(newValue.doubleValue() * r.getWidth());
            }
        });
        r.getStyleClass().add("side-split-pane-comp");
        return new Structure(sidebar, c, r, r.getDividers().get(0));
    }

    public SideSplitPaneComp withInitialWidth(double val) {
        this.initialWidth = val;
        return this;
    }

    public SideSplitPaneComp withOnDividerChange(Consumer<Double> onDividerChange) {
        this.onDividerChange = onDividerChange;
        return this;
    }

    @Value
    public static class Structure implements CompStructure<SplitPane> {

        Region left;
        Region center;
        SplitPane pane;
        SplitPane.Divider divider;

        @Override
        public SplitPane get() {
            return pane;
        }
    }
}
