package io.xpipe.app.comp.storage;

import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataFlow;
import io.xpipe.extension.fxcomps.SimpleComp;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Map;

public class DataSourceTypeComp extends SimpleComp {

    public static final Map<DataSourceType, String> ICONS = Map.of(
            DataSourceType.TABLE, "mdi2t-table-large",
            DataSourceType.STRUCTURE, "mdi2b-beaker-outline",
            DataSourceType.TEXT, "mdi2t-text-box",
            DataSourceType.RAW, "mdi2c-card-outline",
            DataSourceType.COLLECTION, "mdi2b-briefcase-outline");
    private static final String MISSING_ICON = "mdi2c-comment-question-outline";
    private static final Color MISSING_COLOR = Color.RED;
    private static final Map<DataSourceType, Color> COLORS = Map.of(
            DataSourceType.TABLE, Color.rgb(0, 160, 0, 0.5),
            DataSourceType.STRUCTURE, Color.ORANGERED,
            DataSourceType.TEXT, Color.LIGHTBLUE,
            DataSourceType.RAW, Color.GREY,
            DataSourceType.COLLECTION, Color.ORCHID.deriveColor(0, 1.0, 0.85, 1.0));
    private final DataSourceType type;
    private final DataFlow flow;

    public DataSourceTypeComp(DataSourceType type, DataFlow flow) {
        this.type = type;
        this.flow = flow;
    }

    @Override
    protected Region createSimple() {
        var bg = new Region();
        bg.setBackground(new Background(new BackgroundFill(
                type != null ? COLORS.get(type) : MISSING_COLOR, new CornerRadii(12), Insets.EMPTY)));
        bg.getStyleClass().add("background");

        var sp = new StackPane(bg);
        sp.setAlignment(Pos.CENTER);
        sp.getStyleClass().add("data-source-type-comp");

        var icon = new FontIcon(type != null ? ICONS.get(type) : MISSING_ICON);
        icon.iconSizeProperty().bind(Bindings.divide(sp.heightProperty(), 2));
        sp.getChildren().add(icon);

        if (flow == DataFlow.INPUT || flow == DataFlow.INPUT_OUTPUT) {
            var flowIcon = createInputFlowType();
            sp.getChildren().add(flowIcon);
        }

        if (flow == DataFlow.OUTPUT || flow == DataFlow.INPUT_OUTPUT) {
            var flowIcon = createOutputFlowType();
            sp.getChildren().add(flowIcon);
        }

        if (flow == DataFlow.TRANSFORMER) {
            var flowIcon = createTransformerFlowType();
            sp.getChildren().add(flowIcon);
        }

        return sp;
    }

    private Region createInputFlowType() {
        var icon = new FontIcon("mdi2c-chevron-double-left");
        icon.setIconColor(Color.WHITE);
        var anchorPane = new AnchorPane(icon);
        AnchorPane.setLeftAnchor(icon, 3.0);
        AnchorPane.setBottomAnchor(icon, 3.0);
        return anchorPane;
    }

    private Region createOutputFlowType() {
        var icon = new FontIcon("mdi2c-chevron-double-right");
        icon.setIconColor(Color.WHITE);
        var anchorPane = new AnchorPane(icon);
        AnchorPane.setRightAnchor(icon, 3.0);
        AnchorPane.setBottomAnchor(icon, 3.0);
        return anchorPane;
    }

    private Region createTransformerFlowType() {
        var icon = new FontIcon("mdi2t-transfer");
        icon.setIconColor(Color.WHITE);
        var anchorPane = new AnchorPane(icon);
        AnchorPane.setRightAnchor(icon, 3.0);
        AnchorPane.setBottomAnchor(icon, 3.0);
        return anchorPane;
    }
}
