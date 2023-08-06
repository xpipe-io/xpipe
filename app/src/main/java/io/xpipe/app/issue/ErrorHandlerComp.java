package io.xpipe.app.issue;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.TitledPaneComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.util.JfxHelper;
import io.xpipe.app.util.PlatformState;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static atlantafx.base.theme.Styles.ACCENT;
import static atlantafx.base.theme.Styles.BUTTON_OUTLINED;

public class ErrorHandlerComp extends SimpleComp {

    private static final AtomicBoolean showing = new AtomicBoolean(false);
    private final ErrorEvent event;
    private final Stage stage;
    private final Property<ErrorAction> takenAction = new SimpleObjectProperty<>();

    public ErrorHandlerComp(ErrorEvent event, Stage stage) {
        this.event = event;
        this.stage = stage;
    }

    public static void showAndTryWait(ErrorEvent event, boolean forceWait) {
        if (PlatformState.getCurrent() != PlatformState.RUNNING || event.isOmitted()) {
            ErrorAction.ignore().handle(event);
            return;
        }

        if (Platform.isFxApplicationThread()) {
            showAndWaitWithPlatformThread(event, forceWait);
        } else {
            showAndWaitWithOtherThread(event);
        }
    }

    private static Comp<?> setUpComp(ErrorEvent event, Stage w, CountDownLatch finishLatch) {
        var c = new ErrorHandlerComp(event, w);
        w.setOnHidden(e -> {
            if (c.takenAction.getValue() == null) {
                ErrorAction.ignore().handle(event);
                c.takenAction.setValue(ErrorAction.ignore());
            }

            showing.set(false);
            finishLatch.countDown();
        });
        return c;
    }

    public static void showAndWaitWithPlatformThread(ErrorEvent event, boolean forceWait) {
        var finishLatch = new CountDownLatch(1);
        if (!showing.get()) {
            showing.set(true);
            var window = AppWindowHelper.sideWindow(
                    AppI18n.get("errorHandler"),
                    w -> {
                        return setUpComp(event, w, finishLatch);
                    },
                    true,
                    null);

            // An exception is thrown when show and wait is called
            // within an animation or layout processing task, so use show
            try {
                if (forceWait) {
                    window.showAndWait();
                } else {
                    window.show();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public static void showAndWaitWithOtherThread(ErrorEvent event) {
        var showLatch = new CountDownLatch(1);
        var finishLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            if (!showing.get()) {
                showing.set(true);
                var window = AppWindowHelper.sideWindow(
                        AppI18n.get("errorHandler"),
                        w -> {
                            return setUpComp(event, w, finishLatch);
                        },
                        true,
                        null);
                // An exception is thrown when show and wait is called
                // within an animation or layout processing task, so use show
                try {
                    showLatch.countDown();
                    window.show();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });

        try {
            // Only wait for a certain time in case we somehow deadlocked the platform thread
            if (showLatch.await(5, TimeUnit.SECONDS)) {
                finishLatch.await();
            } else {
                TrackEvent.error("Platform thread in error handler was timed out");
            }
        } catch (InterruptedException ignored) {
        }
    }

    private Region createActionComp(ErrorAction a) {
        var r = JfxHelper.createNamedEntry(a.getName(), a.getDescription());
        var b = new ButtonComp(null, r, () -> {
            takenAction.setValue(a);
            if (a.handle(event)) {
                stage.close();
            }
        });
        b.apply(GrowAugment.create(true, false));
        return b.createRegion();
    }

    private Region createDetails() {
        var content = new ErrorDetailsComp(event);
        var tp = new TitledPaneComp(AppI18n.observable("errorDetails"), content, 250);
        var r = tp.createRegion();
        r.getStyleClass().add("details");
        return r;
    }

    private Region createTop() {
        var headerId = event.isTerminal() ? "terminalErrorOccured" : "errorOccured";
        var desc = event.getDescription();
        if (desc == null && event.getThrowable() != null) {
            var tName = event.getThrowable().getClass().getSimpleName();
            desc = AppI18n.get("errorTypeOccured", tName);
        }
        if (desc == null) {
            desc = AppI18n.get("errorNoDetail");
        }

        var graphic = new FontIcon("mdomz-warning");
        graphic.setIconColor(Color.RED);

        var header = new Label(AppI18n.get(headerId), graphic);
        header.setGraphicTextGap(6);
        AppFont.setSize(header, 3);
        var descriptionField = new TextArea(desc);
        descriptionField.setPrefRowCount(6);
        descriptionField.setWrapText(true);
        descriptionField.setEditable(false);
        descriptionField.setPadding(Insets.EMPTY);
        AppFont.small(descriptionField);
        var text = new VBox(header, descriptionField);
        text.setFillWidth(true);
        text.setSpacing(8);
        return text;
    }

    @Override
    protected Region createSimple() {
        var top = createTop();
        var content = new VBox(top, new Separator(Orientation.HORIZONTAL));
        if (event.isReportable()) {
            var header = new Label(AppI18n.get("possibleActions"));
            AppFont.header(header);
            var actionBox = new VBox(header);
            actionBox.getStyleClass().add("actions");
            actionBox.setFillWidth(true);

            for (var action :
                    List.of(ErrorAction.sendDiagnostics(), ErrorAction.reportOnGithub(), ErrorAction.ignore())) {
                var ac = createActionComp(action);
                actionBox.getChildren().add(ac);
            }
            actionBox.getChildren().get(1).getStyleClass().addAll(BUTTON_OUTLINED, ACCENT);

            content.getChildren().addAll(actionBox, new Separator(Orientation.HORIZONTAL));
        }

        var details = createDetails();
        content.getStyleClass().add("top");
        content.setFillWidth(true);
        content.setPrefWidth(600);
        content.setPrefHeight(Region.USE_COMPUTED_SIZE);

        var layout = new BorderPane();
        layout.setCenter(content);
        layout.setBottom(details);
        layout.getStyleClass().add("error-handler-comp");

        return layout;
    }
}
