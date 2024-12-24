package io.xpipe.app.issue;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.augment.GrowAugment;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.TitledPaneComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.util.LicenseRequiredException;

import javafx.beans.binding.Bindings;
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

import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static atlantafx.base.theme.Styles.ACCENT;
import static atlantafx.base.theme.Styles.BUTTON_OUTLINED;

public class ErrorHandlerComp extends SimpleComp {

    private final ErrorEvent event;
    private final Runnable closeDialogAction;
    @Getter
    private final Property<ErrorAction> takenAction = new SimpleObjectProperty<>();

    public ErrorHandlerComp(ErrorEvent event, Runnable closeDialogAction) {
        this.event = event;
        this.closeDialogAction = closeDialogAction;
    }

    private Region createActionButtonGraphic(String nameString, String descString) {
        var header = new Label(nameString);
        AppFont.header(header);
        var desc = new Label(descString);
        AppFont.small(desc);
        var text = new VBox(header, desc);
        text.setSpacing(2);
        return text;
    }

    private Region createActionComp(ErrorAction a) {
        var r = createActionButtonGraphic(a.getName(), a.getDescription());
        var b = new ButtonComp(null, r, () -> {
            takenAction.setValue(a);
            try {
                if (a.handle(event)) {
                    closeDialogAction.run();
                }
            } catch (Exception ignored) {
                closeDialogAction.run();
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
        var desc = event.getDescription();
        if (desc == null && event.getThrowable() != null) {
            var tName = event.getThrowable().getClass().getSimpleName();
            desc = AppI18n.get("errorTypeOccured", tName);
        }
        if (desc == null) {
            desc = AppI18n.get("errorNoDetail");
        }
        desc = desc.trim();

        if (event.isTerminal()) {
            desc = desc + "\n\n" + AppI18n.get("terminalErrorDescription");
        }

        var descriptionField = new TextArea(desc);
        descriptionField.setPrefRowCount(Math.max(5, Math.min((int) desc.lines().count(), 14)));
        descriptionField.setWrapText(true);
        descriptionField.setEditable(false);
        descriptionField.setPadding(Insets.EMPTY);
        AppFont.small(descriptionField);
        var text = new VBox(descriptionField);
        text.setFillWidth(true);
        text.setSpacing(8);
        return text;
    }

    @Override
    protected Region createSimple() {
        var top = createTop();
        var content = new VBox(top, new Separator(Orientation.HORIZONTAL));
        var header = new Label(AppI18n.get("possibleActions"));
        AppFont.header(header);
        var actionBox = new VBox(header);
        actionBox.getStyleClass().add("actions");
        actionBox.setFillWidth(true);

        if (event.getThrowable() instanceof LicenseRequiredException) {
            event.getCustomActions().add(new ErrorAction() {
                @Override
                public String getName() {
                    return AppI18n.get("upgrade");
                }

                @Override
                public String getDescription() {
                    return AppI18n.get("seeTiers");
                }

                @Override
                public boolean handle(ErrorEvent event) {
                    AppLayoutModel.get().selectLicense();
                    return true;
                }
            });
            event.setDisableDefaultActions(true);
        }

        var custom = event.getCustomActions();
        for (var c : custom) {
            var ac = createActionComp(c);
            ac.getStyleClass().addAll(BUTTON_OUTLINED, ACCENT);
            actionBox.getChildren().add(ac);
        }

        if (!event.isDisableDefaultActions()) {
            for (var action :
                    List.of(ErrorAction.automaticallyReport(), ErrorAction.reportOnGithub(), ErrorAction.ignore())) {
                var ac = createActionComp(action);
                actionBox.getChildren().add(ac);
            }
        } else if (event.getCustomActions().isEmpty()) {
            for (var action : List.of(ErrorAction.ignore())) {
                var ac = createActionComp(action);
                actionBox.getChildren().add(ac);
            }
        }
        actionBox.getChildren().get(1).getStyleClass().addAll(BUTTON_OUTLINED, ACCENT);

        content.getChildren().addAll(actionBox);
        content.getStyleClass().add("top");
        content.setFillWidth(true);
        content.setMinHeight(Region.USE_PREF_SIZE);

        var layout = new BorderPane();
        layout.setCenter(content);
        layout.getStyleClass().add("error-handler-comp");
        layout.getStyleClass().add("background");

        if (event.getThrowable() != null) {
            content.getChildren().add(new Separator(Orientation.HORIZONTAL));
            var details = createDetails();
            AppFont.medium(details);
            layout.setBottom(details);
        }

        return layout;
    }
}
