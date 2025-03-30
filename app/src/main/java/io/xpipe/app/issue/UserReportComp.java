package io.xpipe.app.issue;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.*;
import io.xpipe.app.resources.AppResources;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import atlantafx.base.controls.Popover;
import atlantafx.base.controls.Spacer;

import java.nio.file.Files;
import java.nio.file.Path;

public class UserReportComp extends ModalOverlayContentComp {

    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty text = new SimpleStringProperty();
    private final ListProperty<Path> includedDiagnostics;
    private final ErrorEvent event;

    public UserReportComp(ErrorEvent event) {
        this.event = event;
        this.includedDiagnostics = new SimpleListProperty<>(FXCollections.observableArrayList());
    }

    public static boolean show(ErrorEvent event) {
        var comp = new UserReportComp(event);
        var modal = ModalOverlay.of("errorHandler", comp);
        var sent = new SimpleBooleanProperty();
        modal.addButtonBarComp(privacyPolicy());
        modal.addButtonBarComp(Comp.hspacer());
        modal.addButton(new ModalButton(
                "sendReport",
                () -> {
                    comp.send();
                    sent.set(true);
                },
                true,
                true));
        modal.showAndWait();
        return sent.get();
    }

    private static Comp<?> privacyPolicy() {
        return Comp.of(() -> {
            var dataPolicyButton = new Hyperlink(AppI18n.get("dataHandlingPolicies"));
            AppFontSizes.xs(dataPolicyButton);
            dataPolicyButton.setOnAction(event1 -> {
                AppResources.with(AppResources.XPIPE_MODULE, "misc/report_privacy_policy.md", file -> {
                    var markDown = new MarkdownComp(Files.readString(file), s -> s, true)
                            .apply(struc -> struc.get().setMaxWidth(500))
                            .apply(struc -> struc.get().setMaxHeight(400));
                    var popover = new Popover(markDown.createRegion());
                    popover.setCloseButtonEnabled(true);
                    popover.setHeaderAlwaysVisible(false);
                    popover.setDetachable(true);
                    AppFontSizes.xs(popover.getContentNode());
                    popover.show(dataPolicyButton);
                });
                event1.consume();
            });

            var agree = new Label("Note the issue reporter ");
            var buttons = new HBox(agree, dataPolicyButton);
            buttons.setAlignment(Pos.CENTER_LEFT);
            buttons.setMinWidth(Region.USE_PREF_SIZE);
            return buttons;
        });
    }

    @Override
    protected Region createSimple() {
        var emailHeader = new Label(AppI18n.get("provideEmail"));
        emailHeader.setWrapText(true);
        var email = new TextField();
        this.email.bind(email.textProperty());
        VBox.setVgrow(email, Priority.ALWAYS);

        var infoHeader = new Label(AppI18n.get("additionalErrorInfo"));
        var tf = new TextArea();
        text.bind(tf.textProperty());
        VBox.setVgrow(tf, Priority.ALWAYS);

        var attachmentsHeader = new Label(AppI18n.get("additionalErrorAttachments"));
        var attachments = new ListSelectorComp<>(
                        FXCollections.observableList(event.getAttachments()),
                        file -> {
                            if (file.equals(AppLogs.get().getSessionLogsDirectory())) {
                                return AppI18n.get("logFilesAttachment");
                            }

                            return file.getFileName().toString();
                        },
                        includedDiagnostics,
                        file -> false,
                        () -> false)
                .styleClass("attachment-list")
                .createRegion();

        var reportSection = new VBox(
                infoHeader,
                tf,
                new Spacer(8, Orientation.VERTICAL),
                attachmentsHeader,
                new Spacer(3, Orientation.VERTICAL),
                attachments);
        reportSection.setSpacing(5);
        reportSection.getStyleClass().add("report");

        reportSection.getChildren().addAll(new Spacer(8, Orientation.VERTICAL), emailHeader, email);

        var layout = new BorderPane();
        layout.setCenter(reportSection);
        layout.getStyleClass().add("error-report");
        layout.getStyleClass().add("background");
        layout.setPrefWidth(600);
        layout.setPrefHeight(550);
        return layout;
    }

    private void send() {
        event.clearAttachments();
        event.setShouldSendDiagnostics(true);
        includedDiagnostics.forEach(event::addAttachment);
        event.attachUserReport(email.get(), text.get());
        SentryErrorHandler.getInstance().handle(event);
    }
}
