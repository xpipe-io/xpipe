package io.xpipe.app.update;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.Hyperlinks;

import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class UpdateAvailableDialog {

    public static void showIfNeeded() {
        UpdateHandler uh = XPipeDistributionType.get().getUpdateHandler();
        if (uh.getPreparedUpdate().getValue() == null) {
            return;
        }

        // Check whether we still have the latest version prepared
        uh.refreshUpdateCheckSilent(false, uh.getPreparedUpdate().getValue().isSecurityOnly());
        if (uh.getPreparedUpdate().getValue() == null) {
            return;
        }

        TrackEvent.withInfo("Showing update alert ...")
                .tag("version", uh.getPreparedUpdate().getValue().getVersion())
                .handle();
        var u = uh.getPreparedUpdate().getValue();

        var comp = Comp.of(() -> {
            var markdown = new MarkdownComp(u.getBody() != null ? u.getBody() : "", s -> s, false).createRegion();
            return markdown;
        });
        var modal = ModalOverlay.of("updateReadyAlertTitle", comp.prefWidth(600), null);
        for (var action : uh.createActions()) {
            modal.addButton(action);
        }
        AppDialog.show(modal);
    }
}
