package io.xpipe.app.update;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.issue.ErrorAction;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.Hyperlinks;

public class UpdateChangelogDialog {

    private static boolean shown = false;

    public static void showIfNeeded() {
        var update = AppDistributionType.get().getUpdateHandler().getPerformedUpdate();
        if (update != null && !AppDistributionType.get().getUpdateHandler().isUpdateSucceeded()) {
            ErrorEventFactory.fromMessage(AppI18n.get("updateFail"))
                    .documentationLink(DocumentationLink.UPDATE_FAIL)
                    .customAction(ErrorAction.translated("updateFailAction", () -> {
                        Hyperlinks.open(Hyperlinks.GITHUB_LATEST);
                        return true;
                    }))
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
            var markdown = new MarkdownComp(update.getRawDescription(), s -> s, false).createRegion();
            return markdown;
        });
        var modal = ModalOverlay.of("updateChangelogAlertTitle", comp.prefWidth(600), null);
        modal.addButton(ModalButton.ok());
        AppDialog.show(modal);
    }
}
