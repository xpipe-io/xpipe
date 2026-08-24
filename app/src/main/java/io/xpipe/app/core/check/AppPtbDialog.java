package io.xpipe.app.core.check;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.window.AppDialog;

public class AppPtbDialog {

    public static void showIfNeeded() {
        if (!AppProperties.get().isStaging()) {
            return;
        }

        if (AppProperties.get().isAotTrainMode()) {
            return;
        }

        if (!AppProperties.get().isNewBuildSession()) {
            return;
        }

        var content = AppDialog.dialogTextKey("ptbNoticeContent");
        var modal = ModalOverlay.of("ptbNoticeTitle", content);
        modal.persist();
        modal.addButton(ModalButton.quit());
        modal.addButton(ModalButton.ok());
        AppDialog.showAndWait(modal);
    }
}
