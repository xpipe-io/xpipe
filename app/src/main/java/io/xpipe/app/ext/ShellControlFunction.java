package io.xpipe.app.ext;

import io.xpipe.core.process.ShellControl;

public interface ShellControlFunction {

    ShellControl control() throws Exception;
}
