package io.xpipe.app.core;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.OsType;

public class AppAotTrain {

    public static void runTrainingMode() throws Throwable {
        // Linux runners don't support graphics
        if (OsType.getLocal() == OsType.LINUX) {
            return;
        }

        OperationMode.switchToSyncOrThrow(OperationMode.GUI);
        ThreadHelper.sleep(5000);
        BrowserFullSessionModel.DEFAULT.openFileSystemSync(
                DataStorage.get().local().ref(),
                m -> m.getFileSystem().getShell().orElseThrow().view().userHome(),
                null,
                true);
        AppLayoutModel.get().selectSettings();
        ThreadHelper.sleep(1000);
        AppLayoutModel.get().selectLicense();
        ThreadHelper.sleep(1000);
        AppLayoutModel.get().selectBrowser();
        ThreadHelper.sleep(5000);
    }
}
