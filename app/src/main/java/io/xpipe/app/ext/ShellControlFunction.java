package io.xpipe.app.ext;

import io.xpipe.app.process.ShellControl;

public interface ShellControlFunction {

    ShellControl control() throws Exception;
}
