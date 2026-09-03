package io.xpipe.app.process;

import io.xpipe.app.store.ShellStore;

public interface ShellControlParentStoreFunction extends ShellControlFunction {

    default ShellControl control() throws Exception {
        return control(getParentStore().standaloneControl());
    }

    ShellControl control(ShellControl parent) throws Exception;

    ShellStore getParentStore();
}
