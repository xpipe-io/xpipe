package io.xpipe.app.issue;

import io.xpipe.app.core.*;
import io.xpipe.app.util.PlatformState;

import java.util.function.Consumer;

public class GuiErrorHandlerBase {

    protected boolean startupGui(Consumer<Throwable> onFail) {
        var ex = PlatformState.initPlatform();
        if (ex.isPresent()) {
            onFail.accept(ex.get());
            return false;
        }

        try {
            AppProperties.init();
            AppState.init();
            AppExtensionManager.init(false);
            AppI18n.init();
            AppStyle.init();
            AppTheme.init();
        } catch (Throwable r) {
            onFail.accept(r);
            return false;
        }

        return true;
    }
}
