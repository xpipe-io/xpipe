package io.xpipe.app.core;

import com.jfoenix.controls.JFXCheckBox;
import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.fxcomps.Comp;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;

import java.nio.file.Files;
import java.util.List;
import java.util.function.UnaryOperator;

public class AppGreetings {

    public static TitledPane createIntroduction() {
        var tp = new TitledPane();
        tp.setExpanded(true);
        tp.setText(AppI18n.get("introduction"));
        tp.setAlignment(Pos.CENTER_LEFT);
        AppFont.normal(tp);

        AppResources.with(AppResources.XPIPE_MODULE, "misc/welcome.md", file -> {
            var md = Files.readString(file);
            var markdown = new MarkdownComp(md, UnaryOperator.identity()).createRegion();
            tp.setContent(markdown);
        });

        return tp;
    }

    private static TitledPane createPrivacyPolicy() {
        var tp = new TitledPane();
        tp.setExpanded(false);
        tp.setText(AppI18n.get("privacyPolicy"));
        tp.setAlignment(Pos.CENTER_LEFT);
        AppFont.normal(tp);

        AppResources.with(AppResources.XPIPE_MODULE, "misc/privacy.md", file -> {
            var md = Files.readString(file);
            var markdown = new MarkdownComp(md, UnaryOperator.identity()).createRegion();
            tp.setContent(markdown);
        });

        return tp;
    }

    private static TitledPane createEULA() {
        var tp = new TitledPane();
        tp.setExpanded(false);
        tp.setText(AppI18n.get("eula"));
        tp.setAlignment(Pos.CENTER_LEFT);
        AppFont.normal(tp);

        AppResources.with(AppResources.XPIPE_MODULE, "misc/eula.md", file -> {
            var md = Files.readString(file);
            var markdown = new MarkdownComp(md, UnaryOperator.identity()).createRegion();
            tp.setContent(markdown);
        });

        return tp;
    }

    public static void showIfNeeded() {
        // TODO
        if (!AppProperties.get().isImage() || true) {
            return;
        }

        boolean set = AppCache.get("legalAccepted", Boolean.class, () -> false);
        if (set) {
            return;
        }
        var accepted = new SimpleBooleanProperty();
        AppWindowHelper.showAlert(
                alert -> {
                    alert.setTitle(AppI18n.get("greetingsAlertTitle"));
                    alert.setAlertType(Alert.AlertType.NONE);
                    alert.initModality(Modality.APPLICATION_MODAL);

                    var content = List.of(createIntroduction(), createEULA(), createPrivacyPolicy());
                    var accordion = new Accordion(content.toArray(TitledPane[]::new));
                    accordion.setExpandedPane(content.get(0));

                    var acceptanceBox = Comp.of(() -> {
                                var cb = new JFXCheckBox();
                                cb.selectedProperty().bindBidirectional(accepted);

                                var label = new Label(AppI18n.get("legalAccept"));
                                label.setGraphic(cb);
                                AppFont.medium(label);
                                label.setPadding(new Insets(40, 0, 10, 0));
                                label.setOnMouseClicked(event -> accepted.set(!accepted.get()));
                                return label;
                            })
                            .createRegion();

                    var layout = new BorderPane();
                    layout.getStyleClass().add("window-content");
                    layout.setCenter(accordion);
                    layout.setBottom(acceptanceBox);
                    layout.setPrefWidth(700);
                    layout.setPrefHeight(600);

                    alert.getDialogPane().setContent(layout);

                    var buttonType = new ButtonType(AppI18n.get("confirm"), ButtonBar.ButtonData.OK_DONE);
                    alert.getButtonTypes().add(buttonType);
                    var button = alert.getDialogPane().lookupButton(buttonType);
                    button.disableProperty().bind(accepted.not());
                },
                null,
                r -> r.filter(b -> b.getButtonData().isDefaultButton() && accepted.get())
                        .ifPresentOrElse(
                                t -> {
                                    AppCache.update("legalAccepted", true);
                                },
                                OperationMode::close));
    }
}
