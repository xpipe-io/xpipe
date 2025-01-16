package io.xpipe.app.core.mode;

import io.xpipe.app.util.PlatformInit;

public abstract class PlatformMode extends OperationMode {

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public void onSwitchTo() throws Throwable {
        OperationMode.BACKGROUND.onSwitchTo();
        PlatformInit.init(true);
    }

    @Override
    public void finalTeardown() throws Throwable {
        onSwitchFrom();
        BACKGROUND.finalTeardown();
    }
}
