package io.xpipe.app.issue;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.TitledPaneComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.util.JfxHelper;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static atlantafx.base.theme.Styles.ACCENT;
import static atlantafx.base.theme.Styles.BUTTON_OUTLINED;

public class ErrorHandlerComp extends SimpleComp {

    private static final AtomicBoolean showing = new AtomicBoolean(false);
    private final ErrorEvent event;
    private final Stage stage;

    public ErrorHandlerComp(ErrorEvent event, Stage stage) {
        this.event = event;
        this.stage = stage;
    }

    public static void showAndWait(ErrorEvent event) {
        // Always run later to prevent any issues when an exception
        // is thrown within an animation or layout processing task
        // Otherwise, the show and wait method might fail
        PlatformThread.alwaysRunLaterBlocking(() -> {
            synchronized (showing) {
                if (!showing.get()) {
                    showing.set(true);
                    var window = AppWindowHelper.sideWindow(
                            AppI18n.get("errorHandler"), w -> new ErrorHandlerComp(event, w), true, null);
                    window.setOnHidden(e -> {
                        showing.set(false);
                    });
                    window.showAndWait();
                }
            }
        });
    }

    private Region createActionComp(ErrorAction a) {
        var r = JfxHelper.createNamedEntry(a.getName(), a.getDescription());
        var b = new ButtonComp(null, r, () -> {
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

    @Override
    protected Region createSimple() {
        var graphic = new FontIcon("mdomz-warning");
        graphic.setIconColor(Color.RED);

        var headerId = event.isTerminal() ? "terminalErrorOccured" : "errorOccured";
        var desc = event.getDescription();
        if (desc == null && event.getThrowable() != null) {
            var tName = event.getThrowable().getClass().getSimpleName();
            desc = AppI18n.get("errorTypeOccured", tName);
        }
        if (desc == null) {
            desc = AppI18n.get("errorNoDetail");
        }
        var limitedDescription = desc.substring(0, Math.min(1000, desc.length()));
        var top = JfxHelper.createNamedEntry(AppI18n.get(headerId), limitedDescription, graphic);

        var content = new VBox(top, new Separator(Orientation.HORIZONTAL));
        if (event.isReportable()) {
            var header = new Label(AppI18n.get("possibleActions"));
            AppFont.header(header);
            var actionBox = new VBox(header);
            actionBox.getStyleClass().add("actions");
            actionBox.setFillWidth(true);

            for (var action : List.of(ErrorAction.sendDiagnostics(), ErrorAction.ignore())) {
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
        layout.maxHeightProperty().addListener((c, o, n) -> {
            int a = 0;
        });

        return layout;
    }
}
