package io.xpipe.app.storage;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.LoadingIconComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.*;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;

import java.nio.file.Files;

public class DataStorageMigratedDialog {

    public static void show(boolean auth, boolean temp) {
        var def = AppInstallation.ofDefault(AppProperties.get().isStaging());
        var content = AppDialog.dialogTextKey(auth ? "storageMigratedAuthContent" : "storageMigratedContent");
        var modal = ModalOverlay.of(auth ? "storageMigratedAuthTitle" : "storageMigratedTitle", content);
        if (temp && Files.exists(def.getDaemonExecutablePath())) {
            modal.addButton(new ModalButton("restart", () -> {
                AppOperationMode.executeAfterShutdown(() -> {
                    var cmd = CommandBuilder.of().addFile(def.getDaemonExecutablePath());
                    ExternalApplicationHelper.startAsync(cmd);
                });
            }, true, true));
        } else {
            modal.addButton(ModalButton.ok(() -> {
                ThreadHelper.runFailableAsync(() -> {
                    AppDistributionType.get().getUpdateHandler().refreshUpdateCheck(true, false);
                });
            }));
        }
        modal.show();
    }
}
