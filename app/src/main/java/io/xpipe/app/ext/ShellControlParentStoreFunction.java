package io.xpipe.app.ext;

import io.xpipe.core.process.ShellControl;

public interface ShellControlParentStoreFunction extends ShellControlFunction {

    default ShellControl standaloneControl() throws Exception {
        return control(getParentStore().shellFunction().standaloneControl());
    }

    @Override
    default ShellControl tempControl() throws Exception {
        return control(getParentStore().getOrStartSession());
    }

    ShellControl control(ShellControl parent) throws Exception;

    ShellStore getParentStore();
}
