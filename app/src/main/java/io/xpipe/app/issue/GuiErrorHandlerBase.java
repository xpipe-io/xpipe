package io.xpipe.app.issue;

import io.xpipe.app.core.*;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.util.PlatformInit;

import java.util.function.Consumer;

public class GuiErrorHandlerBase {

    protected boolean startupGui(Consumer<Throwable> onFail) {
        try {
            AppProperties.init();
            AppExtensionManager.init();
            PlatformInit.init(true);
            AppMainWindow.init(true);
        } catch (Throwable ex) {
            onFail.accept(ex);
            return false;
        }

        return true;
    }
}
