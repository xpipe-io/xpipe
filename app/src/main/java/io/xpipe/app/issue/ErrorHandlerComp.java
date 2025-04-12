package io.xpipe.app.issue;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.augment.GrowAugment;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.util.LicenseRequiredException;
import io.xpipe.app.util.ThreadHelper;

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
        AppFontSizes.xl(header);
        var desc = new Label(descString);
        AppFontSizes.xs(desc);
        var text = new VBox(header, desc);
        text.setSpacing(2);
        return text;
    }

    private Region createActionComp(ErrorAction a) {
        var graphic = createActionButtonGraphic(a.getName(), a.getDescription());
        var b = new ButtonComp(null, graphic, () -> {
            takenAction.setValue(a);
            ThreadHelper.runAsync(() -> {
                try {
                    var r = a.handle(event);
                    if (r) {
                        closeDialogAction.run();
                    }
                } catch (Exception ignored) {
                    closeDialogAction.run();
                }
            });
        });
        b.apply(GrowAugment.create(true, false));
        return b.createRegion();
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
        descriptionField.setPrefRowCount(Math.max(5, Math.min((int) desc.lines().count() + 1, 14)));
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
        var content = new VBox(top);
        var header = new Label(AppI18n.get("possibleActions"));
        header.setPadding(new Insets(0, 0, 2, 3));
        AppFontSizes.xl(header);
        var actionBox = new VBox();
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
        }

        var custom = event.getCustomActions();
        for (var c : custom) {
            var ac = createActionComp(c);
            ac.getStyleClass().addAll(BUTTON_OUTLINED, ACCENT);
            actionBox.getChildren().add(ac);
        }

        if (event.getLink() != null) {
            actionBox.getChildren().add(createActionComp(ErrorAction.openDocumentation(event.getLink())));
        }

        var hasCustomActions = event.getCustomActions().size() > 0 || event.getLink() != null;
        if (hasCustomActions) {
            actionBox.getChildren().add(createActionComp(ErrorAction.ignore()));
        }

        if (actionBox.getChildren().size() > 0) {
            actionBox.getChildren().addFirst(header);
            content.getChildren().add(new Separator(Orientation.HORIZONTAL));
            actionBox.getChildren().get(1).getStyleClass().addAll(BUTTON_OUTLINED);
            content.getChildren().addAll(actionBox);
        }

        content.getStyleClass().add("top");
        content.setFillWidth(true);
        content.setMinHeight(Region.USE_PREF_SIZE);

        var layout = new VBox();
        layout.getChildren().add(content);
        layout.getStyleClass().add("error-handler-comp");

        return layout;
    }
}
