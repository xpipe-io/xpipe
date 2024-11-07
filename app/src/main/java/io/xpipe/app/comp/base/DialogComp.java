package io.xpipe.app.comp.base;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;

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

import atlantafx.base.theme.Styles;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public abstract class DialogComp extends Comp<CompStructure<Region>> {

    public static void showWindow(String titleKey, Function<Stage, DialogComp> f) {
        var loading = new SimpleBooleanProperty();
        var dialog = new AtomicReference<DialogComp>();
        Platform.runLater(() -> {
            var stage = AppWindowHelper.sideWindow(
                    AppI18n.get(titleKey),
                    window -> {
                        var c = f.apply(window);
                        dialog.set(c);
                        loading.bind(c.busy());
                        return c;
                    },
                    false,
                    loading);
            stage.setOnCloseRequest(event -> {
                if (dialog.get() != null) {
                    dialog.get().discard();
                }
            });
            stage.show();
        });
    }

    protected Region createNavigation() {
        HBox buttons = new HBox();
        buttons.setFillHeight(true);
        var customButton = bottom();
        if (customButton != null) {
            var c = customButton.createRegion();
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
                        .map(buttonComp -> buttonComp.createRegion())
                        .toList());
        var nextButton = finishButton();
        buttons.getChildren().add(nextButton.createRegion());
        return buttons;
    }

    protected Comp<?> finishButton() {
        return new ButtonComp(AppI18n.observable(finishKey()), null, this::finish)
                .apply(struc -> struc.get().setDefaultButton(true))
                .styleClass(Styles.ACCENT)
                .styleClass("next");
    }

    protected String finishKey() {
        return "finishStep";
    }

    protected List<Comp<?>> customButtons() {
        return List.of();
    }

    @Override
    public CompStructure<Region> createBase() {
        var sp = pane(content()).styleClass("dialog-content").createRegion();
        VBox vbox = new VBox();
        vbox.getChildren().addAll(sp, createNavigation());
        vbox.getStyleClass().add("dialog-comp");
        vbox.setFillWidth(true);
        VBox.setVgrow(sp, Priority.ALWAYS);
        return new SimpleCompStructure<>(vbox);
    }

    protected ObservableValue<Boolean> busy() {
        return new SimpleBooleanProperty(false);
    }

    protected abstract void finish();

    protected abstract void discard();

    public abstract Comp<?> content();

    protected Comp<?> pane(Comp<?> content) {
        var entry = content;
        return Comp.of(() -> {
            var entryR = entry.createRegion();
            var sp = new ScrollPane(entryR);
            sp.setFitToWidth(true);
            entryR.minHeightProperty().bind(sp.heightProperty());
            return sp;
        });
    }

    public Comp<?> bottom() {
        return null;
    }
}
