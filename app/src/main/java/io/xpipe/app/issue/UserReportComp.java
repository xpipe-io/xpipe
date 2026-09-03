package io.xpipe.app.issue;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.*;
import io.xpipe.app.util.Hyperlinks;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;

import java.nio.file.Path;

public class UserReportComp extends ModalOverlayContentComp {

    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty text = new SimpleStringProperty();
    private final ListProperty<Path> includedDiagnostics;
    private final ErrorEvent event;
    private final boolean troubleshoot;

    public UserReportComp(ErrorEvent event, boolean troubleshoot) {
        this.event = event;
        this.troubleshoot = troubleshoot;
        this.includedDiagnostics = new SimpleListProperty<>(FXCollections.observableArrayList());
    }

    public static boolean show(ErrorEvent event, boolean troubleshoot) {
        var comp = new UserReportComp(event, troubleshoot);
        var modal = ModalOverlay.of(troubleshoot ? "reportError" : "errorHandler", comp);
        var sent = new SimpleBooleanProperty();
        modal.addButtonBarComp(privacyPolicy());
        modal.addButtonBarComp(RegionBuilder.hspacer());
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

    private static BaseRegionBuilder<?, ?> privacyPolicy() {
        return RegionBuilder.of(() -> {
            var dataPolicyButton = new Hyperlink(AppI18n.get("dataHandlingPolicies"));
            AppFontSizes.xs(dataPolicyButton);
            dataPolicyButton.setOnAction(e -> {
                Hyperlinks.open(Hyperlinks.REPORTER_PRIVACY_POLICY);
                e.consume();
            });

            var buttons = new HBox(dataPolicyButton);
            buttons.setAlignment(Pos.CENTER_LEFT);
            buttons.setMinWidth(Region.USE_PREF_SIZE);
            return buttons;
        });
    }

    @Override
    protected Region createSimple() {
        var emailHeader = new Label(AppI18n.get("provideEmail"));
        emailHeader.setWrapText(true);
        var emailFooter = new Label(AppI18n.get("emailAnonymous"));
        emailFooter.getStyleClass().add(Styles.TEXT_MUTED);
        emailFooter.setWrapText(true);
        var email = new TextField();
        email.promptTextProperty().bind(AppI18n.observable("provideEmailPrompt"));
        this.email.bind(email.textProperty());
        VBox.setVgrow(email, Priority.ALWAYS);

        var infoHeader = new Label(AppI18n.get(troubleshoot ? "describeYourIssue" : "additionalErrorInfo"));
        var tf = new TextArea();
        tf.setWrapText(true);
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
                        file -> null,
                        includedDiagnostics,
                        file -> false,
                        () -> false)
                .style("attachment-list")
                .build();

        var reportSection = new VBox(
                infoHeader,
                tf,
                new Spacer(8, Orientation.VERTICAL),
                attachmentsHeader,
                new Spacer(3, Orientation.VERTICAL),
                attachments);
        reportSection.setSpacing(5);
        reportSection.getStyleClass().add("report");
        reportSection.getChildren().addAll(new Spacer(8, Orientation.VERTICAL), emailHeader, email, emailFooter);
        reportSection.setPrefWidth(600);
        reportSection.setPrefHeight(550);
        return reportSection;
    }

    private void send() {
        event.clearAttachments();
        event.setShouldSendDiagnostics(true);
        includedDiagnostics.forEach(event::addAttachment);
        event.attachUserReport(email.get(), text.get());
        SentryErrorHandler.getInstance().handle(event);
    }
}
