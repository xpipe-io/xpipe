package io.xpipe.app.ext;

import io.xpipe.core.process.ShellControl;

public interface ShellControlParentStoreFunction extends ShellControlFunction {

    default ShellControl control() throws Exception {
        return control(getParentStore().standaloneControl());
    }

    ShellControl control(ShellControl parent) throws Exception;

    ShellStore getParentStore();
}
