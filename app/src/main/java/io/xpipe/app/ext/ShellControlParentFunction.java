package io.xpipe.app.ext;

import io.xpipe.core.process.ShellControl;

public interface ShellControlParentFunction extends ShellControlFunction{

    default ShellControl control() throws Exception {
        return control(parentControl());
    }

    ShellControl control(ShellControl parent) throws Exception;

    ShellControl parentControl() throws Exception;
}
