package io.xpipe.app.ext;

import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.LicensedFeature;

public interface ShellControlFunction {

    ShellControl control() throws Exception;
}
