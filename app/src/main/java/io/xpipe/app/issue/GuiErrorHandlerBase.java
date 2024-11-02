package io.xpipe.app.issue;

import io.xpipe.app.core.*;
import io.xpipe.app.util.PlatformState;

import java.util.function.Consumer;

public class GuiErrorHandlerBase {

    protected boolean startupGui(Consumer<Throwable> onFail) {
        try {
            PlatformState.initPlatformOrThrow();
            AppProperties.init();
            AppExtensionManager.init(false);
            AppI18n.init();
            AppStyle.init();
            AppTheme.init();
        } catch (Throwable ex) {
            onFail.accept(ex);
            return false;
        }

        return true;
    }
}
