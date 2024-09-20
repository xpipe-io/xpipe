package io.xpipe.app.comp.store;

import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.fxcomps.impl.TooltipAugment;
import io.xpipe.app.resources.SystemIcons;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import lombok.AllArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;

@AllArgsConstructor
public class StoreIconComp extends SimpleComp {

    private final StoreEntryWrapper wrapper;
    private final int w;
    private final int h;

    @Override
    protected Region createSimple() {
        var icon = Bindings.createStringBinding(
                () -> {
                    return getImage();
                },
                wrapper.getIcon());
        var imageComp = PrettyImageHelper.ofFixedSize(icon, w, h);
        var storeIcon = imageComp.createRegion();
        if (wrapper.getValidity().getValue().isUsable()) {
            new TooltipAugment<>(wrapper.getEntry().getProvider().displayName(), null).augment(storeIcon);
        }

        var dots = new FontIcon("mdi2d-dots-horizontal");
        dots.setIconSize((int) (h * 1.3));

        var stack = new StackPane(storeIcon, dots);
        stack.setMinHeight(w + 7);
        stack.setMinWidth(w + 7);
        stack.setMaxHeight(w + 7);
        stack.setMaxWidth(w + 7);
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
                StoreIconChoiceDialogComp.show(wrapper.getEntry());
                event.consume();
            }
        });

        return stack;
    }

    private String getImage() {
        if (wrapper.disabledProperty().get()) {
            return "disabled_icon.png";
        }

        if (wrapper.getIcon().getValue() == null) {
            return wrapper.getEntry()
                    .getProvider()
                    .getDisplayIconFileName(wrapper.getEntry().getStore());
        }

        SystemIcons.load();
        return "app:system/" + wrapper.getIcon().getValue() + ".svg";
    }
}
