package io.xpipe.app.issue;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.comp.base.TitledPaneComp;
import io.xpipe.app.core.*;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.resources.AppResources;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import atlantafx.base.controls.Popover;
import atlantafx.base.controls.Spacer;

import java.nio.file.Files;
import java.nio.file.Path;

public class UserReportComp extends SimpleComp {

    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty text = new SimpleStringProperty();
    private final ListProperty<Path> includedDiagnostics;
    private final ErrorEvent event;
    private final Stage stage;

    private boolean sent;

    public UserReportComp(ErrorEvent event, Stage stage) {
        this.event = event;
        this.includedDiagnostics = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.stage = stage;
        stage.setOnHidden(event1 -> {
            if (!sent) {
                ErrorAction.ignore().handle(event);
            }
        });
    }

    public static void show(ErrorEvent event) {
        var window =
                AppWindowHelper.sideWindow(AppI18n.get("errorHandler"), w -> new UserReportComp(event, w), true, null);
        window.showAndWait();
    }

    private Comp<?> createAttachments() {
        var list = new ListSelectorComp<>(
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
                .styleClass("attachment-list");
        return new TitledPaneComp(AppI18n.observable("additionalErrorAttachments"), list, 100)
                .apply(struc -> struc.get().setExpanded(true))
                .apply(s -> AppFont.medium(s.get()))
                .styleClass("attachments");
    }

    @Override
    protected Region createSimple() {
        var emailHeader = new Label(AppI18n.get("provideEmail"));
        emailHeader.setWrapText(true);
        AppFont.medium(emailHeader);
        var email = new TextField();
        this.email.bind(email.textProperty());
        VBox.setVgrow(email, Priority.ALWAYS);

        var header = new Label(AppI18n.get("additionalErrorInfo"));
        AppFont.medium(header);
        var tf = new TextArea();
        text.bind(tf.textProperty());
        VBox.setVgrow(tf, Priority.ALWAYS);
        var reportSection = new VBox(header, tf, new Spacer(8, Orientation.VERTICAL));
        reportSection.setSpacing(5);
        reportSection.getStyleClass().add("report");

        var at = createAttachments().createRegion();

        var buttons = createBottomBarNavigation();

        if (event.getAttachments().size() > 0) {
            reportSection.getChildren().add(at);
        }

        reportSection.getChildren().addAll(new Spacer(8, Orientation.VERTICAL), emailHeader, email);

        var layout = new BorderPane();
        layout.setCenter(reportSection);
        layout.setBottom(buttons);
        layout.getStyleClass().add("error-report");
        layout.getStyleClass().add("background");
        layout.setPrefWidth(600);
        layout.setPrefHeight(550);
        return layout;
    }

    private Region createBottomBarNavigation() {
        var dataPolicyButton = new Hyperlink(AppI18n.get("dataHandlingPolicies"));
        AppFont.small(dataPolicyButton);
        dataPolicyButton.setOnAction(event1 -> {
            AppResources.with(AppResources.XPIPE_MODULE, "misc/report_privacy_policy.md", file -> {
                var markDown = new MarkdownComp(Files.readString(file), s -> s)
                        .apply(struc -> struc.get().setMaxWidth(500))
                        .apply(struc -> struc.get().setMaxHeight(400));
                var popover = new Popover(markDown.createRegion());
                popover.setCloseButtonEnabled(true);
                popover.setHeaderAlwaysVisible(false);
                popover.setDetachable(true);
                AppFont.small(popover.getContentNode());
                popover.show(dataPolicyButton);
            });
            event1.consume();
        });
        var sendButton = new ButtonComp(AppI18n.observable("sendReport"), null, this::send)
                .apply(struc -> struc.get().setDefaultButton(true))
                .createRegion();
        var spacer = new Region();
        var agree = new Label("Note the issue reporter ");
        var buttons = new HBox(agree, dataPolicyButton, spacer, sendButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.getStyleClass().add("buttons");
        HBox.setHgrow(spacer, Priority.ALWAYS);
        AppFont.medium(dataPolicyButton);
        return buttons;
    }

    private void send() {
        event.clearAttachments();
        event.setShouldSendDiagnostics(true);
        includedDiagnostics.forEach(event::addAttachment);
        event.attachUserReport(email.get(), text.get());
        SentryErrorHandler.getInstance().handle(event);
        sent = true;
        stage.close();
    }
}
