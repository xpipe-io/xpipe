package io.xpipe.app.issue;

import io.sentry.Sentry;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.comp.base.TitledPaneComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.extension.I18n;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.SimpleComp;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class UserReportComp extends SimpleComp {

    private final StringProperty text = new SimpleStringProperty();
    private final List<Path> includedDiagnostics;
    private final ErrorEvent event;
    private final Stage stage;

    public UserReportComp(ErrorEvent event, Stage stage) {
        this.event = event;
        this.includedDiagnostics = new ArrayList<>(event.getAttachments());
        this.stage = stage;
    }

    public static void show(ErrorEvent event) {
        var window =
                AppWindowHelper.sideWindow(I18n.get("errorHandler"), w -> new UserReportComp(event, w), true, null);
        window.showAndWait();
    }

    private Comp<?> createAttachments() {
        var list = new ListSelectorComp<>(event.getAttachments(), file -> {
            if (file.equals(AppLogs.get().getSessionLogsDirectory())) {
                return I18n.get("logFilesAttachment");
            }

            return file.getFileName().toString();
        }).styleClass("attachment-list");
        var tp = new TitledPaneComp(I18n.observable("additionalErrorAttachments"), list, 100)
                .apply(s -> AppFont.medium(s.get())).styleClass("attachments");
        return tp;
        //
        //        var list = FXCollections.observableList(event.getSensitiveDiagnostics());
        //        return new ListViewComp<>(list, list, null, a -> {
        //            var label = new Label(a.getFileName().toString());
        //            var cb = new JFXCheckBox();
        //            cb.setSelected(includedDiagnostics.contains(a));
        //            cb.selectedProperty().addListener((c,o,n) -> {
        //                if (n) {
        //                    includedDiagnostics.add(a);
        //                } else {
        //                    includedDiagnostics.remove(a);
        //                }
        //            });
        //            var spacer = new Region();
        //            var c = new HBox(label, spacer, cb);
        //            HBox.setHgrow(spacer, Priority.ALWAYS);
        //            return WrapperComp.of(() -> c);
        //        });
    }

    @Override
    protected Region createSimple() {
        var header = new Label(I18n.get("additionalErrorInfo"));
        AppFont.medium(header);
        var tf = new TextArea();
        text.bind(tf.textProperty());
        VBox.setVgrow(tf, Priority.ALWAYS);
        var reportSection = new VBox(header, tf);
        reportSection.setSpacing(5);
        reportSection.getStyleClass().add("report");

        var at = createAttachments().createRegion();

        var buttons = createBottomBarNavigation();

        if (event.getAttachments().size() > 0) {
            reportSection.getChildren().add(at);
        }

        var layout = new BorderPane();
        layout.setCenter(reportSection);
        layout.setBottom(buttons);
        layout.getStyleClass().add("error-report");
        layout.setPrefWidth(AppFont.em(35));
        layout.setPrefHeight(AppFont.em(25));
        return layout;
    }

    private Region createBottomBarNavigation() {
        var dataPolicyButton = new Hyperlink(I18n.get("dataHandlingPolicies"));
        dataPolicyButton.setOnAction(event1 -> {
            Hyperlinks.open(Hyperlinks.DOCS_PRIVACY);
        });
        var sendButton = new ButtonComp(I18n.observable("sendReport"), null, this::send).createRegion();
        var spacer = new Region();
        var buttons = new HBox(dataPolicyButton, spacer, sendButton);
        buttons.getStyleClass().add("buttons");
        HBox.setHgrow(spacer, Priority.ALWAYS);
        AppFont.medium(dataPolicyButton);
        return buttons;
    }

    private void send() {
        Sentry.withScope(scope -> {
            event.clearAttachments();
            includedDiagnostics.forEach(event::addAttachment);
            SentryErrorHandler.report(event, text.get());
        });

        stage.close();
    }
}
