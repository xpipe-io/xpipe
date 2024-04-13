package io.xpipe.app.core.check;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppWindowHelper;
import javafx.scene.control.Alert;

public class AppPtbCheck {

    public static void check() {
        if (!AppProperties.get().isStaging()) {
            return;
        }

        AppWindowHelper.showBlockingAlert(alert -> {
            alert.setAlertType(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Notice for the public test build");
            alert.getDialogPane()
                    .setContent(AppWindowHelper.alertContentText("You are running a PTB build of XPipe."
                            + " This version is unstable and might contain bugs."
                            + " You should not use it as a daily driver."
                            + " It will also not receive regular updates after its testing period."
                            + " You will have to install and launch the normal XPipe release for that."));
        });
    }
}
