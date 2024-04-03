package io.xpipe.app.update;

import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class UpdateAvailableAlert {

    public static void showIfNeeded() {
        UpdateHandler uh = XPipeDistributionType.get().getUpdateHandler();
        if (uh.getPreparedUpdate().getValue() == null) {
            return;
        }

        // Check whether we still have the latest version prepared
        uh.refreshUpdateCheckSilent();
        if (uh.getPreparedUpdate().getValue() == null) {
            return;
        }

        var u = uh.getPreparedUpdate().getValue();
        var update = AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("updateReadyAlertTitle"));
                    alert.setAlertType(Alert.AlertType.NONE);
                    var markdown =
                            new MarkdownComp(u.getBody() != null ? u.getBody() : "", s -> "&nbsp;" + s).createRegion();
                    alert.getButtonTypes().clear();
                    var updaterContent = uh.createInterface();
                    if (updaterContent != null) {
                        var stack = new StackPane(updaterContent);
                        stack.setPadding(new Insets(18));
                        var box = new VBox(markdown, stack);
                        box.setFillWidth(true);
                        box.setPadding(Insets.EMPTY);
                        alert.getDialogPane().setContent(box);
                    } else {
                        alert.getDialogPane().setContent(markdown);
                        alert.getButtonTypes()
                                .add(new ButtonType(AppI18n.get("install"), ButtonBar.ButtonData.OK_DONE));
                    }

                    alert.getButtonTypes().add(new ButtonType(AppI18n.get("ignore"), ButtonBar.ButtonData.NO));
                })
                .map(buttonType -> buttonType.getButtonData().isDefaultButton())
                .orElse(false);
        if (update) {
            uh.executeUpdateAndClose();
        }
    }
}
