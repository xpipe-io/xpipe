package io.xpipe.app.ext;

import io.xpipe.core.process.ShellControl;

public class StubShellControl extends WrapperShellControl {

    public StubShellControl(ShellControl parent) {
        super(parent);
    }

    @Override
    public void close() throws Exception {}
}
