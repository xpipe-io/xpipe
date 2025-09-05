package io.xpipe.app.core.mode;

import io.xpipe.app.platform.PlatformInit;

public abstract class AppPlatformMode extends AppOperationMode {

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public void onSwitchTo() throws Throwable {
        AppOperationMode.BACKGROUND.onSwitchTo();
        PlatformInit.init(true);
    }

    @Override
    public void finalTeardown() throws Throwable {
        onSwitchFrom();
        BACKGROUND.finalTeardown();
    }
}
