package io.xpipe.app.comp.base;

import atlantafx.base.theme.Styles;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.TooltipAugment;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.javafx.StackedFontIcon;

@Getter
public class TtyWarningComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var fi = new FontIcon("mdi2l-lightning-bolt");
        fi.getStyleClass().add("inner-icon");

        var border = new FontIcon("mdi2s-square-rounded-outline");
        border.getStyleClass().add("outer-icon");
        border.setOpacity(0.5);

        var bg = new FontIcon("mdi2s-square-rounded");
        bg.getStyleClass().add("background-icon");

        var pane = new StackedFontIcon();
        pane.getChildren().addAll(bg, fi, border);
        pane.setAlignment(Pos.CENTER);

        var style =
                """
            .stacked-ikonli-font-icon > .outer-icon { -fx-icon-color: -color-danger-emphasis; }
            
            .stacked-ikonli-font-icon > .outer-icon {
                -fx-icon-size: 26px;
            }
            .stacked-ikonli-font-icon > .background-icon {
                -fx-icon-size: 26px;
                -fx-icon-color: -color-danger-9;
            }
            .stacked-ikonli-font-icon > .inner-icon {
                -fx-icon-size: 12px;
            }
            """;
        pane.getStylesheets().add(Styles.toDataURI(style));
        new TooltipAugment<>("ttyWarning", null).augment(pane);
        return pane;
    }
}
