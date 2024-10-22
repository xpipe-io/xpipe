package io.xpipe.app.ext;

import io.xpipe.core.process.ShellControl;

public interface ShellControlParentStoreFunction extends ShellControlFunction{

    default ShellControl control() throws Exception {
        return control(parentControl());
    }

    ShellControl control(ShellControl parent) throws Exception;

    default ShellControl parentControl() throws Exception {
        return getParentStore().getOrStartSession();
    }

    ShellStore getParentStore();
}
