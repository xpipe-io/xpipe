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

        var content = AppDialog.dialogText("You are running a PTB build of XPipe."
                + " This version is unstable and might contain bugs."
                + " You should not use it as a daily driver."
                + " It will also not receive regular updates after its testing period."
                + " You will have to install and launch the normal XPipe release for that.");
        var modal = ModalOverlay.of("ptbNotice", content);
        modal.persist();
        modal.addButton(ModalButton.quit());
        modal.addButton(ModalButton.ok());
        AppDialog.showAndWait(modal);
    }
}
