package io.xpipe.app.comp.base;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.core.AppI18n;

import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import atlantafx.base.theme.Styles;
import org.int4.fx.builders.common.AbstractRegionBuilder;

import java.util.List;

public abstract class DialogComp extends RegionBuilder<Region> {

    protected Region createNavigation() {
        HBox buttons = new HBox();
        buttons.setFillHeight(true);
        var customButton = bottom();
        if (customButton != null) {
            var c = customButton.build();
            buttons.getChildren().add(c);
            HBox.setHgrow(c, Priority.ALWAYS);
        }
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.SOMETIMES);
        buttons.getChildren().add(spacer);
        buttons.getStyleClass().add("buttons");
        buttons.setSpacing(5);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        buttons.getChildren()
                .addAll(customButtons().stream()
                        .map(buttonComp -> buttonComp.build())
                        .toList());
        var nextButton = finishButton();
        buttons.getChildren().add(nextButton.build());
        return buttons;
    }

    protected AbstractRegionBuilder<?, ?> finishButton() {
        return new ButtonComp(AppI18n.observable(finishKey()), this::finish)
                .style(Styles.ACCENT)
                .style("next");
    }

    protected String finishKey() {
        return "finishStep";
    }

    protected List<AbstractRegionBuilder<?, ?>> customButtons() {
        return List.of();
    }

    @Override
    public Region createSimple() {
        var sp = pane(content()).style("dialog-content").build();
        VBox vbox = new VBox();
        vbox.getChildren().addAll(sp, createNavigation());
        vbox.getStyleClass().add("dialog-comp");
        vbox.setFillWidth(true);
        VBox.setVgrow(sp, Priority.ALWAYS);
        return vbox;
    }

    protected abstract void finish();

    public abstract AbstractRegionBuilder<?, ?> content();

    protected AbstractRegionBuilder<?, ?> pane(AbstractRegionBuilder<?, ?> content) {
        var entry = content;
        return RegionBuilder.of(() -> {
            var entryR = entry.build();
            var sp = new ScrollPane(entryR);
            sp.setFitToWidth(true);
            entryR.minHeightProperty().bind(sp.heightProperty());
            return sp;
        });
    }

    public AbstractRegionBuilder<?, ?> bottom() {
        return null;
    }
}
