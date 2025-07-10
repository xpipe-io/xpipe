package io.xpipe.app.core;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.core.window.AppDialog;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.nio.file.Files;
import java.util.List;
import java.util.function.UnaryOperator;

public class AppGreetingsDialog {

    public static TitledPane createIntroduction() {
        var tp = new TitledPane();
        tp.setExpanded(true);
        tp.setText(AppI18n.get("introduction"));
        tp.setAlignment(Pos.CENTER_LEFT);

        AppResources.with(AppResources.XPIPE_MODULE, "misc/welcome.md", file -> {
            var md = Files.readString(file);
            var markdown = new MarkdownComp(md, UnaryOperator.identity(), true).createRegion();
            tp.setContent(markdown);
        });

        return tp;
    }

    private static TitledPane createEula() {
        var tp = new TitledPane();
        tp.setExpanded(false);
        tp.setText(AppI18n.get("eula"));
        tp.setAlignment(Pos.CENTER_LEFT);

        AppResources.with(AppResources.XPIPE_MODULE, "misc/eula.md", file -> {
            var md = Files.readString(file);
            var markdown = new MarkdownComp(md, UnaryOperator.identity(), true).createRegion();
            tp.setContent(markdown);
        });

        return tp;
    }

    public static void showAndWaitIfNeeded() {
        boolean set = AppCache.getBoolean("legalAccepted", false);
        if (set
                || AppProperties.get().isDevelopmentEnvironment()
                || AppProperties.get().isTest()) {
            return;
        }

        if (AppProperties.get().isAutoAcceptEula()) {
            AppCache.update("legalAccepted", true);
            return;
        }

        var read = new SimpleBooleanProperty();
        var accepted = new SimpleBooleanProperty();

        var modal = ModalOverlay.of(Comp.of(() -> {
            var content = List.of(createIntroduction(), createEula());
            var accordion = new Accordion(content.toArray(TitledPane[]::new));
            accordion.setExpandedPane(content.get(0));
            accordion.expandedPaneProperty().addListener((observable, oldValue, newValue) -> {
                if (content.get(1).equals(newValue)) {
                    read.set(true);
                }
            });

            var acceptanceBox = Comp.of(() -> {
                        var cb = new CheckBox();
                        cb.selectedProperty().bindBidirectional(accepted);

                        var label = new Label(AppI18n.get("legalAccept"));
                        label.setGraphic(cb);
                        label.setPadding(new Insets(20, 0, 10, 0));
                        label.setOnMouseClicked(event -> accepted.set(!accepted.get()));
                        label.setGraphicTextGap(10);
                        return label;
                    })
                    .createRegion();

            var layout = new BorderPane();
            layout.setCenter(accordion);
            layout.setBottom(acceptanceBox);
            layout.setPrefWidth(600);
            layout.setPrefHeight(600);
            return layout;
        }));
        modal.persist();
        modal.addButton(ModalButton.quit());
        modal.addButton(ModalButton.confirm(() -> {
                    AppCache.update("legalAccepted", true);
                }))
                .augment(button -> button.disableProperty().bind(accepted.not()));
        AppDialog.showAndWait(modal);

        if (!AppCache.getBoolean("legalAccepted", false)) {
            AppProperties.get().resetInitialLaunch();
            OperationMode.halt(1);
        }
    }
}
