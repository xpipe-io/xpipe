package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.PrettyImageHelper;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import lombok.AllArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;

@AllArgsConstructor
public class StoreCategoryIconComp extends SimpleRegionBuilder {

    private final StoreCategoryWrapper wrapper;
    private final int size;

    @Override
    protected Region createSimple() {
        var imageComp = PrettyImageHelper.ofFixedSize(wrapper.getIconFile(), size, size);
        var storeIcon = imageComp.build();
        storeIcon.setPadding(new Insets(0, 0, 1, 0));

        var dots = new FontIcon("mdi2d-dots-horizontal");
        dots.setIconSize((int) (size * 1.1));

        var stack = new StackPane(storeIcon, dots);
        stack.setMinHeight(size);
        stack.setMinWidth(size);
        stack.setMaxHeight(size);
        stack.setMaxWidth(size);
        stack.getStyleClass().add("icon");
        stack.setAlignment(Pos.CENTER);

        dots.visibleProperty().bind(stack.hoverProperty());
        storeIcon
                .opacityProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> {
                            return stack.isHover() ? 0.5 : 1.0;
                        },
                        stack.hoverProperty()));

        stack.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                StoreIconChoiceDialog.show(wrapper.getCategory());
                event.consume();
            }
        });

        return stack;
    }
}
