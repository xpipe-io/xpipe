package io.xpipe.app.storage;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.LoadingIconComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;

public class DataStorageMigratedDialog {

    public static void show() {
        var content = AppDialog.dialogTextKey("storageMigratedContent");
        var modal = ModalOverlay.of("storageMigratedTitle", content);
        modal.show();
    }
}
