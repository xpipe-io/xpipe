package io.xpipe.app.platform;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.PrettyImageHelper;

import javafx.scene.Node;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Supplier;

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
            var fi = new FontIcon(icon);
            fi.getStyleClass().add("graphic");
            return fi;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class ImageGraphic extends LabelGraphic {

        String file;
        int size;

        @Override
        public Node createGraphicNode() {
            return PrettyImageHelper.ofFixedSizeSquare(file, size)
                    .styleClass("graphic")
                    .createRegion();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class CompGraphic extends LabelGraphic {

        Comp<?> comp;

        @Override
        public Node createGraphicNode() {
            return comp.styleClass("graphic").createRegion();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class NodeGraphic extends LabelGraphic {

        Supplier<Node> node;

        @Override
        public Node createGraphicNode() {
            var n = node.get();
            if (n != null) {
                n.getStyleClass().add("graphic");
            }
            return n;
        }
    }
}
