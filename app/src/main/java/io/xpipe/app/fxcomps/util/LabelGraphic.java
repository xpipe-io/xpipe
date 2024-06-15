package io.xpipe.app.fxcomps.util;

import io.xpipe.app.fxcomps.Comp;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.kordamp.ikonli.javafx.FontIcon;

public abstract class LabelGraphic {

    public static ObservableValue<LabelGraphic> fixedIcon(String icon) {
        return new SimpleObjectProperty<>(new IconGraphic(icon));
    }

    public abstract Node createGraphicNode();

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class IconGraphic extends LabelGraphic {

        String icon;

        @Override
        public Node createGraphicNode() {
            return new FontIcon(icon);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class CompGraphic extends LabelGraphic {

        Comp<?> comp;

        @Override
        public Node createGraphicNode() {
            return comp.createRegion();
        }
    }
}
