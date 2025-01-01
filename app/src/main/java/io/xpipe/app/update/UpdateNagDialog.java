package io.xpipe.app.update;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.Hyperlinks;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.Instant;

public class UpdateNagDialog {

    public static void showIfNeeded() {
        UpdateHandler uh = XPipeDistributionType.get().getUpdateHandler();
        if (uh.getPerformedUpdate() != null || uh.getPreparedUpdate().getValue() != null) {
            AppCache.clear("lastUpdateNag");
            return;
        }

        if (AppPrefs.get().checkForSecurityUpdates().get() || AppPrefs.get().automaticallyUpdate().getValue()) {
            return;
        }

        Instant lastCheck = AppCache.getNonNull("lastUpdateNag", Instant.class,null);
        if (lastCheck == null) {
            AppCache.update("lastUpdateNag", Instant.now());
            return;
        }

        if (Duration.between(lastCheck, Instant.now()).compareTo(Duration.ofDays(90)) < 0) {
            return;
        }

        var text = AppDialog.dialogTextKey("updateNag");
        var modal = ModalOverlay.of("updateNagTitle", text, null);
        modal.addButton(ModalButton.cancel());
        modal.addButton(new ModalButton(
                "updateNagButton",
                () -> {
                    Hyperlinks.open(Hyperlinks.GITHUB_LATEST);
                },
                true,
                true));
        AppDialog.show(modal);
        AppCache.update("lastUpdateNag", Instant.now());
    }
}
