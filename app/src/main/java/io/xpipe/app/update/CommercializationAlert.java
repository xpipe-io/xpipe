package io.xpipe.app.update;

import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.core.*;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;

import java.nio.file.Files;
import java.util.function.UnaryOperator;

public class CommercializationAlert {

    public static void showIfNeeded() {
        if (AppState.get().isInitialLaunch()) {
            AppCache.update("commercializationSeen", true);
            return;
        }

        boolean set = AppCache.get("commercializationSeen", Boolean.class, () -> false);
        if (set) {
            return;
        }

        AppWindowHelper.showBlockingAlert(alert -> {
            alert.setTitle(AppI18n.get("news"));
            alert.setAlertType(Alert.AlertType.NONE);
            alert.initModality(Modality.NONE);

            AppResources.with(AppResources.XPIPE_MODULE, "misc/commercialization.md", file -> {
                var md = Files.readString(file);
                var markdown = new MarkdownComp(md, UnaryOperator.identity()).createRegion();
                alert.getDialogPane().setContent(markdown);
            });

            alert.getButtonTypes().add(new ButtonType(AppI18n.get("gotIt"), ButtonBar.ButtonData.OK_DONE));
        });

        AppCache.update("commercializationSeen", true);
    }
}
