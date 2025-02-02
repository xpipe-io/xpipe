package io.xpipe.app.issue;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.augment.GrowAugment;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.LicenseRequiredException;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import lombok.Getter;

import java.util.List;

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
        AppFontSizes.base(header);
        var desc = new Label(descString);
        AppFontSizes.xs(desc);
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
        var content = new ErrorDetailsComp(event).prefWidth(600).prefHeight(750);
        var modal = ModalOverlay.of("errorDetails", content);
        var button = new ButtonComp(null, new SimpleObjectProperty<>(new LabelGraphic.NodeGraphic(() -> {
            return createActionButtonGraphic(AppI18n.get("showDetails"), AppI18n.get("showDetailsDescription"));
        })), () -> {
            modal.show();
        });
        var r = button.grow(true, false).createRegion();
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
        AppFontSizes.xs(descriptionField);
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
        AppFontSizes.base(header);
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

        var layout = new VBox();
        layout.getChildren().add(content);
        layout.getStyleClass().add("error-handler-comp");

        if (event.getThrowable() != null) {
            content.getChildren().add(new Separator(Orientation.HORIZONTAL));
            var details = createDetails();
            AppFontSizes.sm(details);
            layout.getChildren().add(details);
            layout.prefHeightProperty().bind(content.heightProperty().add(65).add(details.prefHeightProperty()));
        }

        return layout;
    }
}
