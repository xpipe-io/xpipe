package io.xpipe.core.process;

public class StubShellControl extends WrapperShellControl {

    public StubShellControl(ShellControl parent) {
        super(parent);
    }

    @Override
    public void close() throws Exception {}
}
