package io.xpipe.app.comp.base;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.function.Function;

public abstract class DialogComp extends Comp<CompStructure<Region>> {

    public static void showWindow(String titleKey, Function<Stage, DialogComp> f) {
        var loading = new SimpleBooleanProperty();
        Platform.runLater(() -> {
            var stage = AppWindowHelper.sideWindow(
                    AppI18n.get(titleKey),
                    window -> {
                        var c = f.apply(window);
                        loading.bind(c.busy());
                        return c;
                    },
                    false,
                    loading);
            stage.show();
        });
    }

    protected Region createStepNavigation() {
        HBox buttons = new HBox();
        buttons.setFillHeight(true);
        var customButton = bottom();
        if (customButton != null) {
            buttons.getChildren().add(customButton.createRegion());
        }
        buttons.getChildren().add(new Spacer());
        buttons.getStyleClass().add("buttons");
        buttons.setSpacing(5);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        var nextButton = new ButtonComp(AppI18n.observable("finishStep"), null, this::finish)
                .apply(struc -> struc.get().setDefaultButton(true))
                .styleClass(Styles.ACCENT)
                .styleClass("next");
        buttons.getChildren().add(nextButton.createRegion());
        return buttons;
    }

    @Override
    public CompStructure<Region> createBase() {
        var entryR = content().createRegion();
        entryR.getStyleClass().add("dialog-content");

        var sp = new ScrollPane(entryR);
        sp.setFitToWidth(true);
        entryR.minHeightProperty().bind(sp.heightProperty());

        VBox vbox = new VBox();
        vbox.getChildren().addAll(sp, createStepNavigation());
        vbox.getStyleClass().add("dialog-comp");
        vbox.setFillWidth(true);
        VBox.setVgrow(sp, Priority.ALWAYS);
        return new SimpleCompStructure<>(vbox);
    }

    protected ObservableValue<Boolean> busy() {
        return new SimpleBooleanProperty(false);
    }

    protected abstract void finish();

    public abstract Comp<?> content();

    public Comp<?> bottom() {
        return null;
    }
}
