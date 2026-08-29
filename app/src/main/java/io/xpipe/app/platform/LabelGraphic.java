package io.xpipe.app.platform;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.base.PrettyImageHelper;

import javafx.scene.Node;
import javafx.scene.layout.Region;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.javafx.StackedFontIcon;

import java.util.List;
import java.util.function.Supplier;

public interface LabelGraphic {

    static LabelGraphic none() {
        return new LabelGraphic() {

            @Override
            public Node createGraphicNode() {
                return null;
            }
        };
    }

    Node createGraphicNode();

    @Value
    @AllArgsConstructor
    class IconGraphic implements LabelGraphic {

        String icon;
        String styleClass;

        public IconGraphic(String icon) {
            this.icon = icon;
            this.styleClass = null;
        }

        @Override
        public FontIcon createGraphicNode() {
            var fi = new FontIcon(icon);
            fi.getStyleClass().add("graphic");
            if (styleClass != null) {
                fi.getStyleClass().add(styleClass);
            }
            return fi;
        }
    }

    @Value
    @AllArgsConstructor
    class IconStackGraphic implements LabelGraphic {

        List<IconGraphic> icons;

        @Override
        public Node createGraphicNode() {
            var fi = new StackedFontIcon();
            fi.getChildren()
                    .addAll(icons.stream()
                            .map(iconGraphic -> iconGraphic.createGraphicNode())
                            .toList());
            return fi;
        }
    }

    @Value
    class ImageGraphic implements LabelGraphic {

        String file;
        int size;

        @Override
        public Region createGraphicNode() {
            return PrettyImageHelper.ofFixedSizeSquare(file, size)
                    .style("graphic")
                    .build();
        }
    }

    @Value
    class CompGraphic implements LabelGraphic {

        BaseRegionBuilder<?, ?> comp;

        @Override
        public Node createGraphicNode() {
            return comp.style("graphic").build();
        }
    }

    @Value
    class NodeGraphic implements LabelGraphic {

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
