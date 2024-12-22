package io.xpipe.app.update;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.Hyperlinks;

public class UpdateChangelogAlert {

    private static boolean shown = false;

    public static void showIfNeeded() {
        var update = XPipeDistributionType.get().getUpdateHandler().getPerformedUpdate();
        if (update != null && !XPipeDistributionType.get().getUpdateHandler().isUpdateSucceeded()) {
            ErrorEvent.fromMessage(
                            """
            Update installation did not succeed.

            Note that you can also install the latest version manually from %s
            if there are any problems with the automatic update installation.
            """
                                    .formatted(Hyperlinks.GITHUB_LATEST))
                    .handle();
            return;
        }

        if (update == null || update.getRawDescription() == null) {
            return;
        }

        if (shown) {
            return;
        }
        shown = true;

        var comp = Comp.of(() -> {
            var markdown = new MarkdownComp(update.getRawDescription(), s -> s).createRegion();
            return markdown;
        });
        var modal = ModalOverlay.of("updateChangelogAlertTitle", comp.prefWidth(600), null);
        modal.addButton(ModalButton.ok());
        AppDialog.showAndWait(modal);
    }
}
