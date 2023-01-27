package io.xpipe.app.grid;

import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.extension.I18n;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;

public class UpdateChangelogAlert {

    public static void showIfNeeded() {
        var update = AppUpdater.get().getPerformedUpdate();
        if (update == null || update.getRawDescription() == null) {
            return;
        }

        AppWindowHelper.showAlert(
                alert -> {
                    alert.setTitle(I18n.get("updateChangelogAlertTitle"));
                    alert.setAlertType(Alert.AlertType.NONE);
                    alert.initModality(Modality.NONE);

                    var markdown = new MarkdownComp(update.getRawDescription(), s -> {
                                var header = "<h1>" + I18n.get("whatsNew", update.getName()) + "</h1>";
                                return header + s;
                            })
                            .createRegion();
                    alert.getDialogPane().setContent(markdown);

                    alert.getButtonTypes().add(new ButtonType(I18n.get("gotIt"), ButtonBar.ButtonData.OK_DONE));
                },
                r -> r.filter(b -> b.getButtonData().isDefaultButton()).ifPresent(t -> {}));
    }
}
