package io.xpipe.app.ext;

import io.xpipe.core.process.ShellControl;

public interface ShellControlFunction {

    ShellControl standaloneControl() throws Exception;

    ShellControl tempControl() throws Exception;
}
