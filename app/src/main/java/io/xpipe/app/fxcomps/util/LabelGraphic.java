package io.xpipe.app.fxcomps.util;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;

import javafx.scene.Node;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.kordamp.ikonli.javafx.FontIcon;

public abstract class LabelGraphic {

    public static LabelGraphic none() {
        return new LabelGraphic() {

            @Override
            public Node createGraphicNode() {
                return null;
            }
        };
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
    public static class ImageGraphic extends LabelGraphic {

        String file;
        int size;

        @Override
        public Node createGraphicNode() {
            return PrettyImageHelper.ofFixedSizeSquare(file, size).createRegion();
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
